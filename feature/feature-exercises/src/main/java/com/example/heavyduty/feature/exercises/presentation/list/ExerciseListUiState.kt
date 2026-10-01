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
    /** Only used when the screen is opened in exercise-picker (selection) mode. In pick order. */
    val selectedIds: Set<Long> = emptySet(),
    /** Exercise shown in the animated preview dialog, if open. */
    val previewExercise: Exercise? = null,
)

/**
 * User-initiated events from the exercise list screen.
 */
sealed interface ExerciseListUiEvent {
    data class OnSearchQueryChange(val query: String) : ExerciseListUiEvent
    data class OnFilterSelect(val filter: MuscleGroup?) : ExerciseListUiEvent
    data class OnEquipmentFilterSelect(val equipment: String?) : ExerciseListUiEvent
    data class OnExerciseClick(val exerciseId: Long) : ExerciseListUiEvent
    data class OnToggleSelection(val exerciseId: Long) : ExerciseListUiEvent
    data class OnPreviewExercise(val exercise: Exercise?) : ExerciseListUiEvent
}

/**
 * One-shot effects emitted by the ViewModel (navigation, snackbars).
 */
sealed interface ExerciseListUiEffect {
    data class NavigateToDetail(val exerciseId: Long) : ExerciseListUiEffect
    data class ShowError(val message: String) : ExerciseListUiEffect
}
