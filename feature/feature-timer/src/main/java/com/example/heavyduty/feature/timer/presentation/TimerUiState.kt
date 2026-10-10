package com.example.heavyduty.feature.timer.presentation

import com.example.heavyduty.feature.timer.domain.model.TimerMode

/**
 * Immutable UI state for the timer screen.
 */
data class TimerUiState(
    val mode: TimerMode = TimerMode.REST,
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val rounds: Int = 1,
    val currentRound: Int = 1
)

/**
 * User-initiated events from the timer screen.
 */
sealed interface TimerUiEvent {
    data class OnModeSelect(val mode: TimerMode) : TimerUiEvent
    data class OnDurationChange(val seconds: Int) : TimerUiEvent
    data class OnRoundsChange(val rounds: Int) : TimerUiEvent
    data object OnStartPause : TimerUiEvent
    data object OnReset : TimerUiEvent
}

/**
 * One-shot effects for the timer screen.
 */
sealed interface TimerUiEffect {
    data object TimerFinished : TimerUiEffect
    data class ShowMessage(val message: String) : TimerUiEffect
}
