package com.example.heavyduty.feature.timer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.timer.domain.model.TimerMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TimerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var timerJob: Job? = null

    fun onEvent(event: TimerUiEvent) {
        when (event) {
            is TimerUiEvent.OnModeSelect -> selectMode(event.mode)
            is TimerUiEvent.OnDurationChange -> {
                _uiState.update {
                    it.copy(totalSeconds = event.seconds, remainingSeconds = event.seconds)
                }
            }
            is TimerUiEvent.OnRoundsChange -> {
                _uiState.update { it.copy(rounds = event.rounds.coerceAtLeast(1)) }
            }
            TimerUiEvent.OnStartPause -> toggleStartPause()
            TimerUiEvent.OnReset -> reset()
        }
    }

    private fun selectMode(mode: TimerMode) {
        timerJob?.cancel()
        val defaultSeconds = when (mode) {
            TimerMode.REST -> 60
            TimerMode.STOPWATCH -> 0
            TimerMode.AMRAP -> 600 // 10 minutes
            TimerMode.EMOM -> 60
            TimerMode.TABATA -> 20
        }
        val defaultRounds = when (mode) {
            TimerMode.TABATA -> 8
            TimerMode.EMOM -> 10
            else -> 1
        }
        _uiState.update {
            TimerUiState(
                mode = mode,
                totalSeconds = defaultSeconds,
                remainingSeconds = defaultSeconds,
                rounds = defaultRounds,
            )
        }
    }

    private fun toggleStartPause() {
        val state = _uiState.value
        if (state.isRunning) {
            // Pause
            timerJob?.cancel()
            _uiState.update { it.copy(isRunning = false, isPaused = true) }
        } else {
            // Start or resume
            _uiState.update { it.copy(isRunning = true, isPaused = false) }
            when (state.mode) {
                TimerMode.STOPWATCH -> startCountUp()
                else -> startCountDown()
            }
        }
    }

    private fun startCountDown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.isRunning) {
                delay(1000)
                _uiState.update {
                    it.copy(
                        remainingSeconds = it.remainingSeconds - 1,
                        elapsedSeconds = it.elapsedSeconds + 1,
                    )
                }
            }
            if (_uiState.value.remainingSeconds <= 0) {
                val state = _uiState.value
                if (state.currentRound < state.rounds) {
                    // Next round
                    _uiState.update {
                        it.copy(
                            currentRound = it.currentRound + 1,
                            remainingSeconds = it.totalSeconds,
                        )
                    }
                    startCountDown() // Recursive for next round
                } else {
                    _uiState.update { it.copy(isRunning = false) }
                    _uiEffect.emit(TimerUiEffect.TimerFinished)
                }
            }
        }
    }

    private fun startCountUp() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1000)
                _uiState.update {
                    it.copy(elapsedSeconds = it.elapsedSeconds + 1)
                }
            }
        }
    }

    private fun reset() {
        timerJob?.cancel()
        val mode = _uiState.value.mode
        _uiState.update {
            it.copy(
                remainingSeconds = it.totalSeconds,
                elapsedSeconds = 0,
                isRunning = false,
                isPaused = false,
                currentRound = 1,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
