package com.example.heavyduty.feature.profile.domain.model

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus

private const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L
private const val SECONDS_PER_HOUR = 3600f

/**
 * Ids of the working sets that, when lifted, beat the heaviest weight ever logged for their
 * exercise — walking [workingSets] in order, starting from [earlierBests] (the best weight per
 * exercise before them), so an exercise's first set ever counts too.
 */
fun personalRecordSetIds(workingSets: List<WorkoutSet>, earlierBests: Map<Long, Float> = emptyMap()): Set<Long> {
    val bestWeightByExercise = earlierBests.toMutableMap()
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

/** Workouts shown in the profile's activity feed; the full list lives in the history screen. */
private const val FEED_SIZE = 30

/**
 * The finished workouts the profile feed shows, newest first. One still in progress isn't
 * one yet: its sets only count once it's finished, so it showed as "0 min · 0 kg".
 */
fun feedSessions(sessions: List<WorkoutSession>): List<WorkoutSession> = sessions
    .filter { it.status == WorkoutStatus.COMPLETED }
    .sortedByDescending { it.startTime }
    .take(FEED_SIZE)

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
        start to secs / SECONDS_PER_HOUR
    }
}
