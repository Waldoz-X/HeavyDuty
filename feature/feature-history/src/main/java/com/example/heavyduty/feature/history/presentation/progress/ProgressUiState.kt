package com.example.heavyduty.feature.history.presentation.progress

import com.example.heavyduty.core.domain.model.WorkoutSet

data class ProgressUiState(
    val exerciseId: Long = 0,
    val sets: List<WorkoutSet> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface ProgressUiEvent {
    data object OnNavigateBack : ProgressUiEvent
}

sealed interface ProgressUiEffect {
    data object NavigateBack : ProgressUiEffect
}
