package com.example.heavyduty.feature.profile.domain.model

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus

private const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L

/**
 * Ids of the working sets that, when lifted, beat the heaviest weight ever logged for their
 * exercise — walking the history in order, so the first set of an exercise counts too.
 */
fun personalRecordSetIds(workingSets: List<WorkoutSet>): Set<Long> {
    val bestWeightByExercise = mutableMapOf<Long, Float>()
    return workingSets
        .filter { it.weight != null && it.completedAt != null }
        .sortedBy { it.completedAt }
        .mapNotNullTo(mutableSetOf()) { set ->
            val weight = set.weight ?: return@mapNotNullTo null
            if (weight > (bestWeightByExercise[set.exerciseId] ?: 0f)) {
                bestWeightByExercise[set.exerciseId] = weight
                set.id
            } else {
                null
            }
        }
}

/**
 * Hours of completed workouts in each of the last [weeks] seven-day windows ending at [now],
 * **oldest first** (charts read left to right). Each pair is the window's start and its hours.
 */
fun weeklyTrainingHours(sessions: List<WorkoutSession>, now: Long, weeks: Int = 4): List<Pair<Long, Float>> {
    val completed = sessions.filter { it.status == WorkoutStatus.COMPLETED }
    return (weeks downTo 1).map { weeksBack ->
        val start = now - weeksBack * WEEK_MILLIS
        val end = start + WEEK_MILLIS
        val secs = completed.filter { it.startTime >= start && it.startTime < end }.sumOf { it.totalDurationSecs }
        start to secs / 3600f
    }
}
