package com.example.heavyduty.feature.timer.engine

import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.presentation.TimerUiEffect
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import com.example.heavyduty.feature.timer.presentation.TimerUiState
import com.example.heavyduty.feature.timer.service.TimerNotifications
import com.example.heavyduty.feature.timer.service.TimerService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the timer's countdown loop and lifecycle as a process-scoped singleton so it keeps
 * ticking independent of any ViewModel/Activity — those come and go with the screen, this
 * doesn't. [TimerService] observes [state] to keep the process and the phone awake and reflect
 * progress in a notification, and stops itself once the timer is idle; it holds no timing logic
 * of its own.
 */
@Singleton
class TimerEngine @Inject constructor(
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val device: TimerDevice,
    private val notifications: TimerNotifications,
    private val clock: TimerClock
) : RestTimerController {
    private val engineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TimerUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<TimerUiEffect> = _effects.asSharedFlow()

    override val restState: StateFlow<RestTimerState?> = _state
        .map { state ->
            val resting = state.mode == TimerMode.REST &&
                (state.isRunning || state.isPaused) &&
                state.remainingSeconds > 0
            if (resting) RestTimerState(state.remainingSeconds, state.totalSeconds) else null
        }
        .stateIn(engineScope, SharingStarted.Eagerly, null)

    private var tickJob: Job? = null

    private val countdown = Countdown(clock)

    /**
     * The timer tab's own setup, put aside while a workout rest borrows the engine: when that
     * rest ends or is skipped the tab gets it back, instead of keeping the rest's duration
     * (+15 s adjustments included — it used to come back as 1:15 and lose e.g. a Tabata setup).
     */
    private var setupBeforeWorkoutRest: TimerUiState? = null

    fun onEvent(event: TimerUiEvent) {
        when (event) {
            is TimerUiEvent.OnModeSelect -> selectMode(event.mode)

            is TimerUiEvent.OnDurationChange -> {
                _state.update {
                    it.copy(totalSeconds = event.seconds, remainingSeconds = event.seconds)
                }
            }

            is TimerUiEvent.OnRoundsChange -> {
                _state.update { it.copy(rounds = event.rounds.coerceAtLeast(1)) }
            }

            TimerUiEvent.OnStartPause -> toggleStartPause()

            TimerUiEvent.OnReset -> reset()
        }
    }

    /**
     * Starts (or restarts) a rest countdown from outside this module's own screen — see
     * [RestTimerController] doc. Replaces whatever timer was running, same as picking REST
     * mode and pressing start manually.
     */
    override fun startRest(seconds: Int) {
        tickJob?.cancel()
        notifications.cancelRestFinished()
        // Rests can follow each other; keep the setup from before the first one.
        if (setupBeforeWorkoutRest == null) setupBeforeWorkoutRest = _state.value.asIdleSetup()
        _state.update {
            TimerUiState(
                mode = TimerMode.REST,
                totalSeconds = seconds,
                remainingSeconds = seconds,
                rounds = 1,
                isRunning = true
            )
        }
        device.startService()
        startCountDown()
    }

    override fun adjustRest(deltaSeconds: Int) {
        if (restState.value == null) return
        countdown.extendBy(deltaSeconds)
        _state.update {
            it.copy(
                remainingSeconds = (it.remainingSeconds + deltaSeconds).coerceAtLeast(0),
                totalSeconds = (it.totalSeconds + deltaSeconds).coerceAtLeast(1)
            )
        }
    }

    override fun skipRest() {
        if (restState.value == null) return
        reset()
    }

    /** Gives the timer tab its own setup back once a workout rest is over (see [setupBeforeWorkoutRest]). */
    private fun restoreSetupAfterWorkoutRest() {
        val setup = setupBeforeWorkoutRest ?: return
        setupBeforeWorkoutRest = null
        _state.value = setup
    }

    private fun selectMode(mode: TimerMode) {
        tickJob?.cancel()
        // Picking a mode on the timer tab is a new setup: nothing to give back after the rest.
        setupBeforeWorkoutRest = null
        val defaultSeconds = when (mode) {
            TimerMode.REST -> 60
            TimerMode.STOPWATCH -> 0
            TimerMode.AMRAP -> 600
            TimerMode.EMOM -> 60
            TimerMode.TABATA -> 20
        }
        val defaultRounds = when (mode) {
            TimerMode.TABATA -> 8
            TimerMode.EMOM -> 10
            else -> 1
        }
        _state.update {
            TimerUiState(
                mode = mode,
                totalSeconds = defaultSeconds,
                remainingSeconds = defaultSeconds,
                rounds = defaultRounds
            )
        }
    }

    private fun toggleStartPause() {
        val current = _state.value
        if (current.isRunning) {
            tickJob?.cancel()
            _state.update { it.copy(isRunning = false, isPaused = true) }
        } else {
            _state.update { it.copy(isRunning = true, isPaused = false) }
            device.startService()
            when (current.mode) {
                TimerMode.STOPWATCH -> startCountUp()
                else -> startCountDown()
            }
        }
    }

    private fun startCountDown() {
        tickJob?.cancel()
        countdown.startFrom(_state.value.remainingSeconds)
        val show = { seconds: Int -> _state.update { it.showingRemaining(seconds) } }
        tickJob = engineScope.launch {
            countdown.runToZero(show)
            while (_state.value.currentRound < _state.value.rounds) {
                countdown.nextRound(_state.value.totalSeconds)
                _state.update { it.copy(currentRound = it.currentRound + 1, remainingSeconds = it.totalSeconds) }
                device.vibrate()
                countdown.runToZero(show)
            }
            finishCountDown()
        }
    }

    /** Alerts while the state still says running, so [TimerService] keeps the phone awake for it. */
    private suspend fun finishCountDown() {
        val finished = _state.value
        device.vibrateFinished()
        _effects.emit(TimerUiEffect.TimerFinished)
        // Only a rest that ran out — skipping one means the lifter is already on it.
        if (finished.mode == TimerMode.REST) notifications.notifyRestFinished()
        // Back at its start, ready to run again: a timer doesn't sit at 0:00.
        _state.update { it.asIdleSetup() }
        restoreSetupAfterWorkoutRest()
    }

    private fun startCountUp() {
        tickJob?.cancel()
        val startedAt = clock.elapsedMillis() - _state.value.elapsedSeconds * MILLIS_PER_SECOND
        tickJob = engineScope.launch {
            while (true) {
                val runningMillis = clock.elapsedMillis() - startedAt
                _state.update { it.copy(elapsedSeconds = (runningMillis / MILLIS_PER_SECOND).toInt()) }
                delay(MILLIS_PER_SECOND - runningMillis % MILLIS_PER_SECOND)
            }
        }
    }

    private fun reset() {
        tickJob?.cancel()
        _state.update { it.asIdleSetup() }
        restoreSetupAfterWorkoutRest()
    }
}

private fun TimerUiState.showingRemaining(seconds: Int): TimerUiState = copy(
    remainingSeconds = seconds,
    elapsedSeconds = elapsedSeconds + (remainingSeconds - seconds).coerceAtLeast(0)
)

/** The same timer, stopped and back at its start (full duration, round 1). */
private fun TimerUiState.asIdleSetup(): TimerUiState = copy(
    remainingSeconds = totalSeconds,
    elapsedSeconds = 0,
    isRunning = false,
    isPaused = false,
    currentRound = 1
)
