package com.example.heavyduty.feature.routines.presentation.editor

import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutType

/**
 * Immutable UI state for the routine editor screen.
 */
data class RoutineEditorUiState(
    val routineId: Long? = null,
    val name: String = "",
    val description: String = "",
    val type: WorkoutType = WorkoutType.GYM,
    val exercises: List<RoutineExercise> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
)

/**
 * User-initiated events from the routine editor screen.
 */
sealed interface RoutineEditorUiEvent {
    data class OnNameChange(val name: String) : RoutineEditorUiEvent
    data class OnDescriptionChange(val description: String) : RoutineEditorUiEvent
    data class OnTypeChange(val type: WorkoutType) : RoutineEditorUiEvent
    data class OnRemoveExercise(val routineExercise: RoutineExercise) : RoutineEditorUiEvent
    data object OnSave : RoutineEditorUiEvent
    data object OnNavigateBack : RoutineEditorUiEvent
    data object OnNavigateToExercises : RoutineEditorUiEvent
}

/**
 * One-shot effects for the routine editor screen.
 */
sealed interface RoutineEditorUiEffect {
    data object NavigateBack : RoutineEditorUiEffect
    data object NavigateToExercises : RoutineEditorUiEffect
    data class ShowMessage(val message: String) : RoutineEditorUiEffect
}
