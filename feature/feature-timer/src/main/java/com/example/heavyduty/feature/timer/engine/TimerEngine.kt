package com.example.heavyduty.feature.timer.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.presentation.TimerUiEffect
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import com.example.heavyduty.feature.timer.presentation.TimerUiState
import com.example.heavyduty.feature.timer.service.TimerNotifications
import com.example.heavyduty.feature.timer.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
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
 * doesn't. [TimerService] observes [state] purely to keep the process alive in the background
 * and reflect progress in a notification; it holds no timing logic of its own.
 */
@Singleton
class TimerEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val notifications: TimerNotifications
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
        startService()
        startCountDown()
    }

    override fun adjustRest(deltaSeconds: Int) {
        if (restState.value == null) return
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
        stopService()
    }

    private fun toggleStartPause() {
        val current = _state.value
        if (current.isRunning) {
            tickJob?.cancel()
            _state.update { it.copy(isRunning = false, isPaused = true) }
        } else {
            _state.update { it.copy(isRunning = true, isPaused = false) }
            startService()
            when (current.mode) {
                TimerMode.STOPWATCH -> startCountUp()
                else -> startCountDown()
            }
        }
    }

    private fun startCountDown() {
        tickJob?.cancel()
        tickJob = engineScope.launch {
            while (_state.value.remainingSeconds > 0 && _state.value.isRunning) {
                delay(1.seconds)
                _state.update {
                    it.copy(
                        remainingSeconds = it.remainingSeconds - 1,
                        elapsedSeconds = it.elapsedSeconds + 1
                    )
                }
            }
            if (_state.value.remainingSeconds <= 0) {
                val current = _state.value
                if (current.currentRound < current.rounds) {
                    _state.update {
                        it.copy(
                            currentRound = it.currentRound + 1,
                            remainingSeconds = it.totalSeconds
                        )
                    }
                    vibrate()
                    startCountDown()
                } else {
                    _state.update { it.copy(isRunning = false) }
                    vibrate()
                    _effects.emit(TimerUiEffect.TimerFinished)
                    // Only a rest that ran out — skipping one means the lifter is already on it.
                    if (current.mode == TimerMode.REST) notifications.notifyRestFinished()
                    restoreSetupAfterWorkoutRest()
                    stopService()
                }
            }
        }
    }

    private fun startCountUp() {
        tickJob?.cancel()
        tickJob = engineScope.launch {
            while (_state.value.isRunning) {
                delay(1.seconds)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun reset() {
        tickJob?.cancel()
        _state.update { it.asIdleSetup() }
        restoreSetupAfterWorkoutRest()
        stopService()
    }

    /** The same timer, stopped and back at its start (full duration, round 1). */
    private fun TimerUiState.asIdleSetup(): TimerUiState = copy(
        remainingSeconds = totalSeconds,
        elapsedSeconds = 0,
        isRunning = false,
        isPaused = false,
        currentRound = 1
    )

    private fun startService() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }
    }

    private fun stopService() {
        runCatching {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        }
    }
}
