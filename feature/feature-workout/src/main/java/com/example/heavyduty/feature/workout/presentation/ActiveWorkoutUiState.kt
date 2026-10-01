package com.example.heavyduty.feature.workout.presentation

import androidx.annotation.StringRes
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.Prescription
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.repository.RestTimerState

/**
 * Immutable UI state for the active workout screen.
 *
 * [completedSets] always mirrors the database (the ViewModel observes the active session), so
 * every screen showing the workout agrees on it. Everything that depends on it — the current
 * set number, per-exercise progress — is derived below instead of stored, so switching
 * exercises can never leave a stale count behind.
 */
data class ActiveWorkoutUiState(
    val sessionId: Long? = null,
    val sessionName: String = "",
    val routineExercises: List<RoutineExercise> = emptyList(),
    val completedSets: List<WorkoutSet> = emptyList(),
    val currentExerciseIndex: Int = 0,
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
    /** The current exercise's sets from its last previous session, by set number. */
    val previousSets: Map<Int, WorkoutSet> = emptyMap(),
    /** The shared rest countdown, shown on this screen while it runs. */
    val restTimer: RestTimerState? = null,
    /** Routines the start screen offers to begin. */
    val routines: List<Routine> = emptyList(),
    /** True while a set is being saved, so a double tap can't log it twice. */
    val isLoggingSet: Boolean = false,
) {
    val currentExercise: RoutineExercise? get() = routineExercises.getOrNull(currentExerciseIndex)

    /** kg or lb of the equipment used for the current exercise (see [RoutineExercise.displayUnit]). */
    val currentUnit: WeightUnit get() = currentExercise?.displayUnit ?: WeightUnit.KG

    /** Number of the next set to log for the current exercise. */
    val currentSetNumber: Int get() = nextSetNumber(currentExercise?.exerciseId, completedSets)

    fun setsFor(exerciseId: Long): List<WorkoutSet> = completedSets.filter { it.exerciseId == exerciseId }

    /** Sets planned by the routine (0 for a free workout, which has no plan). */
    val plannedSetCount: Int get() = routineExercises.sumOf { it.targetSets }

    /** Planned sets already done — extra sets beyond an exercise's target don't count twice. */
    val completedPlannedSetCount: Int
        get() = routineExercises.sumOf { exercise ->
            setsFor(exercise.exerciseId).count { !it.isWarmup }.coerceAtMost(exercise.targetSets)
        }

    /** Values the current set is pre-filled with when the lifter logs it without typing. */
    val currentSetHint: SetHint
        get() {
            val exercise = currentExercise ?: return SetHint()
            val previous = previousSets[currentSetNumber]
            val suggestion = suggestedPrescription
            return SetHint(
                reps = suggestion?.reps ?: previous?.reps ?: exercise.targetReps.takeIf { it > 0 },
                // 0 kg means "no weight planned/used", not a load to repeat.
                weightKg = suggestion?.weightKg?.takeIf { it > 0f }
                    ?: previous?.weight?.takeIf { it > 0f }
                    ?: exercise.targetWeight.takeIf { it > 0f },
                durationSecs = suggestion?.durationSecs ?: previous?.durationSecs
                    ?: exercise.targetDurationSecs.takeIf { it > 0 },
            )
        }
}

/** Reps/weight/time a set falls back to when its input is left empty. Weight is in kg. */
data class SetHint(
    val reps: Int? = null,
    val weightKg: Float? = null,
    val durationSecs: Int? = null,
)

/** Next set number for [exerciseId]: one past both the count and the highest number logged. */
internal fun nextSetNumber(exerciseId: Long?, completedSets: List<WorkoutSet>): Int {
    if (exerciseId == null) return 1
    val setsForExercise = completedSets.filter { it.exerciseId == exerciseId }
    val maxSet = setsForExercise.maxOfOrNull { it.setNumber } ?: 0
    return maxOf(setsForExercise.size, maxSet) + 1
}

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
    data class OnStartRoutine(val routineId: Long) : ActiveWorkoutUiEvent
    data object OnNavigateToRoutines : ActiveWorkoutUiEvent
    data object OnToggleExercisePicker : ActiveWorkoutUiEvent
    data class OnExercisePickerQueryChange(val query: String) : ActiveWorkoutUiEvent
    data class OnAddExercise(val exercise: Exercise) : ActiveWorkoutUiEvent
    data object OnUseSuggestion : ActiveWorkoutUiEvent
    data class OnAdjustRest(val deltaSeconds: Int) : ActiveWorkoutUiEvent
    data object OnSkipRest : ActiveWorkoutUiEvent
    /** Switches the current exercise between kg and lb — the machine or plates in use. */
    data object OnToggleUnit : ActiveWorkoutUiEvent
}

/**
 * One-shot effects for the active workout screen.
 */
sealed interface ActiveWorkoutUiEffect {
    data object NavigateToRoutines : ActiveWorkoutUiEffect
    data class ShowMessage(@param:StringRes val messageResId: Int) : ActiveWorkoutUiEffect
    data object WorkoutCompleted : ActiveWorkoutUiEffect
}
