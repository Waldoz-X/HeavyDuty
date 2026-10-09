package com.example.heavyduty.feature.exercises.presentation.detail

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.WeightUnit

/**
 * Immutable UI state for the exercise detail screen.
 */
data class ExerciseDetailUiState(
    /** Known from the route before [exercise] loads — the loading header uses it (shared image). */
    val exerciseId: Long = 0,
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
)

/**
 * User-initiated events from the exercise detail screen.
 */
sealed interface ExerciseDetailUiEvent {
    data object OnNavigateBack : ExerciseDetailUiEvent
    data class OnWeightUnitChange(val unit: WeightUnit) : ExerciseDetailUiEvent
}

/**
 * One-shot effects for the exercise detail screen.
 */
sealed interface ExerciseDetailUiEffect {
    data object NavigateBack : ExerciseDetailUiEffect
}
