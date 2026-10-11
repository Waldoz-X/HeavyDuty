package com.example.heavyduty.feature.history.presentation.detail

import com.example.heavyduty.core.domain.model.WorkoutSet

/** One exercise as the share card lists it: how many working sets, and the best of them. */
data class ShareExerciseLine(val name: String, val workingSets: Int, val bestSet: WorkoutSet?)

/** How many exercises the card has room for; the rest are summed up as "+N more". */
const val SHARE_MAX_EXERCISES = 6

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
        bestSet = working
            .filter { it.weight != null || it.reps != null }
            .maxWithOrNull(compareBy<WorkoutSet>({ it.weight ?: 0f }, { it.reps ?: 0 }))
    )
}
