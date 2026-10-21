package com.example.heavyduty.feature.history.presentation.detail

import androidx.annotation.StringRes
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.feature.history.domain.model.ExerciseSets
import com.example.heavyduty.feature.history.domain.model.WorkoutShareSummary
import com.example.heavyduty.feature.history.domain.model.exerciseSets
import com.example.heavyduty.feature.history.domain.model.workingSetCount
import com.example.heavyduty.feature.history.domain.model.workingVolumeKg

data class WorkoutDetailUiState(
    val session: WorkoutSession? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** What the share card shows; null until worked out (it reads past workouts for records). */
    val shareSummary: WorkoutShareSummary? = null,
    /** The silhouette the share card's body map uses, as chosen in Statistics. */
    val bodyGender: BodyGender = BodyGender.MALE
) {
    /** The sets grouped by exercise, in the order the exercises were first trained. */
    val exercises: List<ExerciseSets> get() = session?.exerciseSets().orEmpty()

    /** Working sets (no warm-ups), like everywhere else the app counts sets. */
    val workingSets: Int get() = session?.workingSetCount ?: 0

    /** Kilograms moved in working sets. */
    val volumeKg: Float get() = session?.workingVolumeKg ?: 0f
}

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
