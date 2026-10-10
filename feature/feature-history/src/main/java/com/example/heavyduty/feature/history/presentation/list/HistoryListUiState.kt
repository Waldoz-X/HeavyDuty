package com.example.heavyduty.feature.history.presentation.list

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.feature.history.domain.usecase.WorkoutStats

data class HistoryListUiState(
    val sessions: List<WorkoutSession> = emptyList(),
    val stats: WorkoutStats = WorkoutStats(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false
)

sealed interface HistoryListUiEvent {
    data class OnSessionClick(val sessionId: Long) : HistoryListUiEvent
    data class OnExerciseProgressClick(val exerciseId: Long) : HistoryListUiEvent
}

sealed interface HistoryListUiEffect {
    data class NavigateToProgress(val exerciseId: Long) : HistoryListUiEffect
}
