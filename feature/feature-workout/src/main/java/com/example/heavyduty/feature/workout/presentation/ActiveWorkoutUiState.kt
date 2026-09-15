package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.Prescription
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutSet

/**
 * Immutable UI state for the active workout screen.
 */
data class ActiveWorkoutUiState(
    val sessionId: Long? = null,
    val sessionName: String = "",
    val routineExercises: List<RoutineExercise> = emptyList(),
    val completedSets: List<WorkoutSet> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val repsInput: String = "",
    val weightInput: String = "",
    val isWarmupInput: Boolean = false,
    val effortInput: String = "",
    /** From user preferences — off by default, see feature-profile's "Esfuerzo percibido" card. */
    val effortTrackingEnabled: Boolean = false,
    /** "RIR" or "RPE" — which scale [effortInput] is interpreted in. */
    val preferredEffortScale: String = "RIR",
    /** Per-set toggle for holds/carries — logs durationSecs instead of reps. See [isWorkTimerRunning]. */
    val isTimedInput: Boolean = false,
    val isWorkTimerRunning: Boolean = false,
    val workTimerElapsedSecs: Int = 0,
    val elapsedSeconds: Int = 0,
    val sessionStartTime: Long? = null,
    val isLoading: Boolean = true,
    val isActive: Boolean = false,
    /** True for free workouts, where exercises aren't fixed by a routine and can be added on the fly. */
    val allowAddingExercises: Boolean = false,
    val isPickingExercise: Boolean = false,
    val exercisePickerQuery: String = "",
    val exercisePickerResults: List<Exercise> = emptyList(),
    val error: String? = null,
    /** Suggested weight/reps for the current exercise's next set, from prior sessions only. */
    val suggestedPrescription: Prescription? = null,
)

/**
 * User-initiated events from the active workout screen.
 */
sealed interface ActiveWorkoutUiEvent {
    data class OnRepsChange(val reps: String) : ActiveWorkoutUiEvent
    data class OnWeightChange(val weight: String) : ActiveWorkoutUiEvent
    data object OnToggleWarmupInput : ActiveWorkoutUiEvent
    data class OnEffortInputChange(val value: String) : ActiveWorkoutUiEvent
    data object OnToggleTimedInput : ActiveWorkoutUiEvent
    data object OnToggleWorkTimer : ActiveWorkoutUiEvent
    data object OnLogSet : ActiveWorkoutUiEvent
    data class OnDeleteSet(val set: WorkoutSet) : ActiveWorkoutUiEvent
    data class OnSelectExercise(val index: Int) : ActiveWorkoutUiEvent
    data object OnCompleteWorkout : ActiveWorkoutUiEvent
    data object OnStartFreeWorkout : ActiveWorkoutUiEvent
    data object OnNavigateToRoutines : ActiveWorkoutUiEvent
    data object OnToggleExercisePicker : ActiveWorkoutUiEvent
    data class OnExercisePickerQueryChange(val query: String) : ActiveWorkoutUiEvent
    data class OnAddExercise(val exercise: Exercise) : ActiveWorkoutUiEvent
    data object OnUseSuggestion : ActiveWorkoutUiEvent
}

/**
 * One-shot effects for the active workout screen.
 */
sealed interface ActiveWorkoutUiEffect {
    data object NavigateToRoutines : ActiveWorkoutUiEffect
    data class ShowMessage(val message: String) : ActiveWorkoutUiEffect
    data object WorkoutCompleted : ActiveWorkoutUiEffect
}
