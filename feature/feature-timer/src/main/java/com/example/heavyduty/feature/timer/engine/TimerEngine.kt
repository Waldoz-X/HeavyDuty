package com.example.heavyduty.feature.timer.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import com.example.heavyduty.feature.timer.presentation.TimerUiEffect
import com.example.heavyduty.feature.timer.presentation.TimerUiEvent
import com.example.heavyduty.feature.timer.presentation.TimerUiState
import com.example.heavyduty.feature.timer.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the timer's countdown loop and lifecycle as a process-scoped singleton so it keeps
 * ticking independent of any ViewModel/Activity — those come and go with the screen, this
 * doesn't. [TimerService] observes [state] purely to keep the process alive in the background
 * and reflect progress in a notification; it holds no timing logic of its own.
 */
@Singleton
class TimerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    private val engineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TimerUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<TimerUiEffect> = _effects.asSharedFlow()

    private var tickJob: Job? = null

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

    private fun selectMode(mode: TimerMode) {
        tickJob?.cancel()
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
                rounds = defaultRounds,
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
                delay(1000)
                _state.update {
                    it.copy(
                        remainingSeconds = it.remainingSeconds - 1,
                        elapsedSeconds = it.elapsedSeconds + 1,
                    )
                }
            }
            if (_state.value.remainingSeconds <= 0) {
                val current = _state.value
                if (current.currentRound < current.rounds) {
                    _state.update {
                        it.copy(
                            currentRound = it.currentRound + 1,
                            remainingSeconds = it.totalSeconds,
                        )
                    }
                    vibrate()
                    startCountDown()
                } else {
                    _state.update { it.copy(isRunning = false) }
                    vibrate()
                    _effects.emit(TimerUiEffect.TimerFinished)
                    stopService()
                }
            }
        }
    }

    private fun startCountUp() {
        tickJob?.cancel()
        tickJob = engineScope.launch {
            while (_state.value.isRunning) {
                delay(1000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun reset() {
        tickJob?.cancel()
        _state.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                elapsedSeconds = 0,
                isRunning = false,
                isPaused = false,
                currentRound = 1,
            )
        }
        stopService()
    }

    private fun startService() {
        ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
    }

    private fun stopService() {
        context.stopService(Intent(context, TimerService::class.java))
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun vibrate() {
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
