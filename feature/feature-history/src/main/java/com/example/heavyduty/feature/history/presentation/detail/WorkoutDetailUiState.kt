package com.example.heavyduty.feature.history.presentation.detail

import androidx.annotation.StringRes
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.displayName

data class WorkoutDetailUiState(val session: WorkoutSession? = null, val isLoading: Boolean = true, val loadFailed: Boolean = false) {
    /** The sets grouped by exercise, in the order the exercises were first trained. */
    val exercises: List<ExerciseSets>
        get() = session?.sets.orEmpty()
            .sortedWith(compareBy({ it.completedAt ?: Long.MAX_VALUE }, { it.setNumber }))
            .groupBy { it.exerciseId }
            .map { (exerciseId, sets) ->
                ExerciseSets(exerciseId, sets.first().exercise?.displayName.orEmpty(), sets.sortedBy { it.setNumber })
            }

    /** Working sets (no warm-ups), like everywhere else the app counts sets. */
    val workingSets: Int get() = session?.sets.orEmpty().count { !it.isWarmup }

    /** Kilograms moved in working sets. */
    val volumeKg: Float get() = session?.sets.orEmpty().filter {
        !it.isWarmup
    }.sumOf { (it.weight ?: 0f).toDouble() * (it.reps ?: 0) }.toFloat()
}

data class ExerciseSets(val exerciseId: Long, val name: String, val sets: List<WorkoutSet>)

sealed interface WorkoutDetailUiEvent {
    data class OnSaveDuration(val minutes: String) : WorkoutDetailUiEvent

    /** [weight] as typed, in [unit] — the unit the set was logged in. */
    data class OnSaveSet(val set: WorkoutSet, val weight: String, val reps: String, val unit: WeightUnit) : WorkoutDetailUiEvent
    data class OnDeleteSet(val set: WorkoutSet) : WorkoutDetailUiEvent
    data class OnUndoDeleteSet(val set: WorkoutSet) : WorkoutDetailUiEvent
    data object OnDeleteWorkout : WorkoutDetailUiEvent
}

sealed interface WorkoutDetailUiEffect {
    data object WorkoutDeleted : WorkoutDetailUiEffect
    data class SetDeleted(val set: WorkoutSet) : WorkoutDetailUiEffect
    data class ShowMessage(@param:StringRes val messageResId: Int) : WorkoutDetailUiEffect
}
