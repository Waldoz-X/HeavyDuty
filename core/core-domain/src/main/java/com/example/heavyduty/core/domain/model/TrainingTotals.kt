package com.example.heavyduty.core.domain.model

/**
 * Workout/duration/volume/set totals for a window of training. Shared by every screen that
 * shows "how much did I train in this range" so they all agree on what counts.
 */
data class TrainingTotals(
    val workouts: Int = 0,
    val durationSecs: Int = 0,
    val volumeKg: Float = 0f,
    val sets: Int = 0,
)

/**
 * Only [WorkoutStatus.COMPLETED] sessions count, and only the non-warmup sets that belong to
 * them — sets of an in-progress or abandoned session are left out, so a workout never inflates
 * the totals before it is finished.
 */
fun trainingTotals(sessions: List<WorkoutSession>, sets: List<WorkoutSet>): TrainingTotals {
    val completedSessions = sessions.filter { it.status == WorkoutStatus.COMPLETED }
    val countedSets = sets.countedFor(sessions)
    return TrainingTotals(
        workouts = completedSessions.size,
        durationSecs = completedSessions.sumOf { it.totalDurationSecs },
        volumeKg = countedSets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }.toFloat(),
        sets = countedSets.size,
    )
}

/**
 * The working sets (no warmups) that belong to a [WorkoutStatus.COMPLETED] session among
 * [sessions] — the sets every "how much did I train" number is computed from.
 */
fun List<WorkoutSet>.countedFor(sessions: List<WorkoutSession>): List<WorkoutSet> {
    val completedSessionIds = sessions
        .filter { it.status == WorkoutStatus.COMPLETED }
        .mapTo(mutableSetOf()) { it.id }
    return excludeWarmups().filter { it.sessionId in completedSessionIds }
}
