package com.example.heavyduty.feature.exercises.presentation.detail

import com.example.heavyduty.core.domain.model.Exercise

/**
 * Immutable UI state for the exercise detail screen.
 */
data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * User-initiated events from the exercise detail screen.
 */
sealed interface ExerciseDetailUiEvent {
    data object OnNavigateBack : ExerciseDetailUiEvent
}

/**
 * One-shot effects for the exercise detail screen.
 */
sealed interface ExerciseDetailUiEffect {
    data object NavigateBack : ExerciseDetailUiEffect
}
