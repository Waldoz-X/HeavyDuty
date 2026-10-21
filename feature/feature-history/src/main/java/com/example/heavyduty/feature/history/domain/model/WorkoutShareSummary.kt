package com.example.heavyduty.feature.history.domain.model

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutSet

/** Everything the share card shows about one workout, already worked out. */
data class WorkoutShareSummary(
    val startTime: Long,
    val durationSecs: Int,
    val workingSets: Int,
    val volumeKg: Float,
    val exercises: List<ShareExerciseLine>,
    /** Exercises whose best set beat every earlier workout, in training order. */
    val records: List<ShareRecord>,
    /** Working sets credited to each muscle (fractional for compound lifts), for the body map. */
    val setsByMuscle: Map<MuscleGroup, Float>
)

/** A new record: the exercise and the set that set it. */
data class ShareRecord(val exerciseName: String, val set: WorkoutSet)

/** One exercise as the share card lists it: how many working sets, and the best of them. */
data class ShareExerciseLine(val name: String, val workingSets: Int, val bestSet: WorkoutSet?)

/** How many exercises the card has room for; the rest are summed up as "+N more". */
const val SHARE_MAX_EXERCISES = 6

/** How many records the card lists by name; more are only counted. */
const val SHARE_MAX_RECORDS = 3

/** Heaviest first; at the same weight, more reps. */
val heaviestThenMostReps: Comparator<WorkoutSet> = compareBy<WorkoutSet>({ it.weight ?: 0f }, { it.reps ?: 0 })

/**
 * The share card's exercise lines, in training order: warm-ups don't count as sets, and the best
 * set is the heaviest (more reps breaking a tie). Exercises with only warm-ups are left out.
 */
fun shareExerciseLines(exercises: List<ExerciseSets>): List<ShareExerciseLine> = exercises.mapNotNull { exercise ->
    val working = exercise.sets.filter { !it.isWarmup }
    if (working.isEmpty()) return@mapNotNull null
    ShareExerciseLine(
        name = exercise.name,
        workingSets = working.size,
        bestSet = working.filter { it.weight != null || it.reps != null }.maxWithOrNull(heaviestThenMostReps)
    )
}
