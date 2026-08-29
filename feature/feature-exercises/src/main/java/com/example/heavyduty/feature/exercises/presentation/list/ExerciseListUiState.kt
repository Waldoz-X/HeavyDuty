package com.example.heavyduty.feature.exercises.presentation.list

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.MuscleGroup

/**
 * Immutable UI state for the exercise list screen.
 */
data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: MuscleGroup? = null,
    /** Equipment values present in the current muscle/search filter's results, for the chip row. */
    val availableEquipment: List<String> = emptyList(),
    val selectedEquipment: String? = null,
    val error: String? = null,
)

/**
 * User-initiated events from the exercise list screen.
 */
sealed interface ExerciseListUiEvent {
    data class OnSearchQueryChange(val query: String) : ExerciseListUiEvent
    data class OnFilterSelect(val filter: MuscleGroup?) : ExerciseListUiEvent
    data class OnEquipmentFilterSelect(val equipment: String?) : ExerciseListUiEvent
    data class OnExerciseClick(val exerciseId: Long) : ExerciseListUiEvent
}

/**
 * One-shot effects emitted by the ViewModel (navigation, snackbars).
 */
sealed interface ExerciseListUiEffect {
    data class NavigateToDetail(val exerciseId: Long) : ExerciseListUiEffect
    data class ShowError(val message: String) : ExerciseListUiEffect
}
