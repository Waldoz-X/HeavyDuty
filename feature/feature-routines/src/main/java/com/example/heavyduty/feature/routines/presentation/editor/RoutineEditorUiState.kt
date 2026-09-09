package com.example.heavyduty.feature.routines.presentation.editor

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutType

import com.example.heavyduty.core.domain.model.Routine

/**
 * Immutable UI state for the routine editor screen.
 */
data class RoutineEditorUiState(
    val originalRoutine: Routine? = null,
    val routineId: Long? = null,
    val name: String = "",
    val description: String = "",
    val type: WorkoutType = WorkoutType.GYM,
    val exercises: List<RoutineExercise> = emptyList(),
    val isPickingExercise: Boolean = false,
    val exercisePickerQuery: String = "",
    val exercisePickerResults: List<Exercise> = emptyList(),
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
    data object OnToggleExercisePicker : RoutineEditorUiEvent
    data class OnExercisePickerQueryChange(val query: String) : RoutineEditorUiEvent
    data class OnAddExercise(val exercise: Exercise) : RoutineEditorUiEvent
    data class OnUpdateExercise(val index: Int, val updated: RoutineExercise) : RoutineEditorUiEvent
    data class OnMoveExercise(val index: Int, val direction: Int) : RoutineEditorUiEvent
    data class OnRemoveExercise(val routineExercise: RoutineExercise) : RoutineEditorUiEvent
    data class OnPairWithNext(val index: Int) : RoutineEditorUiEvent
    data class OnUnpair(val index: Int) : RoutineEditorUiEvent
    data object OnSave : RoutineEditorUiEvent
    data object OnNavigateBack : RoutineEditorUiEvent
}

/**
 * One-shot effects for the routine editor screen.
 */
sealed interface RoutineEditorUiEffect {
    data object NavigateBack : RoutineEditorUiEffect
    data class ShowMessage(val message: String) : RoutineEditorUiEffect
}
