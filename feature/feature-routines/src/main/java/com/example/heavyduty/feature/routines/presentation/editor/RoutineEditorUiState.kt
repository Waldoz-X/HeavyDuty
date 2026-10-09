package com.example.heavyduty.feature.routines.presentation.editor

import androidx.annotation.StringRes
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutType

/**
 * Immutable UI state for the routine editor screen.
 *
 * Exercises added in this session carry a temporary negative [RoutineExercise.id] so every row
 * has a stable, unique key for drag-to-reorder; it is reset to 0 on save so the database
 * assigns the real id.
 */
data class RoutineEditorUiState(
    val originalRoutine: Routine? = null,
    val routineId: Long? = null,
    val name: String = "",
    val description: String = "",
    val type: WorkoutType = WorkoutType.GYM,
    val exercises: List<RoutineExercise> = emptyList(),
    /** (sessionStartTime, totalVolumeKg) per completed session of this routine — edit mode only. */
    val volumeHistory: List<Pair<Long, Float>> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val loadFailed: Boolean = false,
    /** Index of the exercise whose settings sheet is open. */
    val editingIndex: Int? = null,
    /** Set when saving was attempted without a name. */
    val showNameError: Boolean = false,
    /** What was last loaded or saved — anything different is an unsaved change. */
    val savedSnapshot: EditorSnapshot = EditorSnapshot(),
) {
    val hasUnsavedChanges: Boolean
        get() = EditorSnapshot(name, description, type, exercises) != savedSnapshot

    val editingExercise: RoutineExercise? get() = editingIndex?.let { exercises.getOrNull(it) }

    val totalSets: Int get() = exercises.sumOf { it.targetSets }

    /** Rough session length: each set ~40 s of work plus its planned rest. */
    val estimatedMinutes: Int
        get() = exercises.sumOf { it.targetSets * (SECONDS_PER_SET + it.restBetweenSetsSecs) } / 60

    private companion object {
        const val SECONDS_PER_SET = 40
    }
}

data class EditorSnapshot(
    val name: String = "",
    val description: String = "",
    val type: WorkoutType = WorkoutType.GYM,
    val exercises: List<RoutineExercise> = emptyList(),
)

/**
 * User-initiated events from the routine editor screen.
 */
sealed interface RoutineEditorUiEvent {
    data class OnNameChange(val name: String) : RoutineEditorUiEvent
    data class OnDescriptionChange(val description: String) : RoutineEditorUiEvent
    data class OnTypeChange(val type: WorkoutType) : RoutineEditorUiEvent
    data class OnUpdateExercise(val index: Int, val updated: RoutineExercise) : RoutineEditorUiEvent
    /** Moves the exercise at [from] to position [to] (drag-and-drop or the row menu). */
    data class OnMoveExercise(val from: Int, val to: Int) : RoutineEditorUiEvent
    data class OnRemoveExercise(val index: Int) : RoutineEditorUiEvent
    data object OnUndoRemove : RoutineEditorUiEvent
    data class OnDuplicateExercise(val index: Int) : RoutineEditorUiEvent
    data class OnPairWithNext(val index: Int) : RoutineEditorUiEvent
    data class OnUnpair(val index: Int) : RoutineEditorUiEvent
    data class OnOpenExercise(val index: Int) : RoutineEditorUiEvent
    data object OnCloseExercise : RoutineEditorUiEvent
    /** Shows every exercise of the routine in [unit]. */
    data class OnSetAllUnits(val unit: WeightUnit) : RoutineEditorUiEvent
    data object OnSave : RoutineEditorUiEvent
    /** Exercises chosen in the picker, in pick order. */
    data class OnExercisesPicked(val exerciseIds: List<Long>) : RoutineEditorUiEvent
}

/**
 * One-shot effects for the routine editor screen.
 */
sealed interface RoutineEditorUiEffect {
    data object NavigateBack : RoutineEditorUiEffect
    data class ShowMessage(@param:StringRes val messageResId: Int) : RoutineEditorUiEffect
    /** An exercise was removed; the snackbar offers to undo it. */
    data class ShowUndoRemove(val exerciseName: String) : RoutineEditorUiEffect
    /** Exercises came back from the picker; the list scrolls to show them. */
    data class ExercisesAdded(val count: Int) : RoutineEditorUiEffect
}
