package com.example.heavyduty.feature.workout.presentation

import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.bestEstimatedOneRepMax
import com.example.heavyduty.core.domain.model.estimatedOneRepMax
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.toKgFrom

/** What the next set is logged with: its reps or hold time, weight in kg, and effort. */
internal data class SetEntry(val reps: Int?, val durationSecs: Int?, val weightKg: Float?, val effort: Effort?)

/**
 * Reads the inputs; an empty one falls back to the hint shown in it (the suggestion, last time,
 * or the plan). Null when there's nothing to log: no reps — or, for a hold, no time.
 */
internal fun ActiveWorkoutUiState.setEntry(): SetEntry? {
    val hint = currentSetHint
    val unit = currentUnit
    val reps = if (isTimedInput) null else repsInput.toIntOrNull() ?: hint.reps
    val durationSecs = if (isTimedInput) workTimerElapsedSecs.takeIf { it > 0 } ?: hint.durationSecs else null
    if (reps == null && durationSecs == null) return null
    // weightInput is typed in the exercise's current unit — always stored in kg. A hint is
    // stored as the rounded value the lifter saw in the field, not the raw conversion.
    val weightKg = (weightInput.toFloatOrNull() ?: hint.weightKg?.kgToRounded(unit))?.toKgFrom(unit)
    val effort = effortInput.toFloatOrNull()?.let { value ->
        if (preferredEffortScale == "RPE") Effort.Rpe(value) else Effort.Rir(value.toInt())
    }
    return SetEntry(reps = reps, durationSecs = durationSecs, weightKg = weightKg, effort = effort)
}

/**
 * The set's estimated 1RM when it beats every one of [earlierSets] — from past sessions and
 * earlier today — else null. A warm-up never counts, and neither does the first time an exercise
 * is done: there's nothing to beat yet.
 */
internal fun newRecordEstimate(set: WorkoutSet, earlierSets: List<WorkoutSet>): Float? {
    if (set.isWarmup) return null
    val estimate = estimatedOneRepMax(set.weight ?: return null, set.reps ?: return null) ?: return null
    val previousBest = earlierSets.bestEstimatedOneRepMax() ?: return null
    return estimate.takeIf { it > previousBest + PR_MIN_GAIN_KG }
}

/** Margin over the previous best e1RM for a set to count as a record (float noise). */
private const val PR_MIN_GAIN_KG = 0.1f
