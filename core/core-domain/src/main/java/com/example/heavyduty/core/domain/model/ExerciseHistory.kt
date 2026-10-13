package com.example.heavyduty.core.domain.model

/**
 * A set at or above this share of its exercise's best estimated 1RM counts as a hard set — the
 * kind that keeps strength "in the bank" (see [strengthRetention]).
 */
const val HARD_SET_INTENSITY = 0.7f

/**
 * One exercise's finished training summed up: how many working sets were logged, and its best
 * estimated 1RM ([estimatedOneRepMax]) with the set it came from — null when no set has a
 * weight and reps it can be estimated from.
 */
data class ExerciseSetSummary(
    val exerciseId: Long,
    val workingSets: Int,
    val bestOneRepMaxKg: Float?,
    val bestSetWeightKg: Float?,
    val bestSetReps: Int?,
    val bestSetAt: Long?
)

/*
 * The functions below define what these numbers mean, over a plain list of completed sets.
 * The database answers the same questions with aggregate queries (so years of history never
 * have to be loaded into memory) and is tested against these, which stay the reference;
 * TrainingStatsFromHistory applies them for the in-memory fakes.
 */

fun exerciseSetSummaries(completedSets: List<WorkoutSet>): List<ExerciseSetSummary> =
    completedSets.excludeWarmups().groupBy { it.exerciseId }.map { (exerciseId, sets) ->
        val best = sets
            .mapNotNull { set -> estimatedOneRepMax(set.weight ?: 0f, set.reps ?: 0)?.let { it to set } }
            .maxByOrNull { (oneRepMax, _) -> oneRepMax }
        ExerciseSetSummary(
            exerciseId = exerciseId,
            workingSets = sets.size,
            bestOneRepMaxKg = best?.first,
            bestSetWeightKg = best?.second?.weight,
            bestSetReps = best?.second?.reps,
            bestSetAt = best?.second?.completedAt
        )
    }

/** When each exercise last had a hard working set ([HARD_SET_INTENSITY]); exercises with none are left out. */
fun lastHardSetAtByExercise(completedSets: List<WorkoutSet>): Map<Long, Long> {
    val bestByExercise = exerciseSetSummaries(completedSets).associate { it.exerciseId to it.bestOneRepMaxKg }
    fun WorkoutSet.isHard(): Boolean {
        val oneRepMax = bestByExercise[exerciseId]?.takeIf { it > 0f } ?: return false
        return (weight ?: 0f) / oneRepMax >= HARD_SET_INTENSITY
    }
    return completedSets.excludeWarmups()
        .filter { it.completedAt != null && it.isHard() }
        .groupBy { it.exerciseId }
        .mapValues { (_, sets) -> sets.maxOf { it.completedAt!! } }
}

/**
 * Heaviest working weight per exercise among the completed sets of finished workouts that
 * started before [time] — what a later set has to beat to be a new record.
 */
fun bestWeightsBefore(sessions: List<WorkoutSession>, completedSets: List<WorkoutSet>, time: Long): Map<Long, Float> {
    val earlierSessions = sessions.filter { it.status == WorkoutStatus.COMPLETED && it.startTime < time }.mapTo(HashSet()) { it.id }
    return completedSets.excludeWarmups()
        .filter { it.sessionId in earlierSessions && it.weight != null && it.completedAt != null }
        .groupBy { it.exerciseId }
        .mapValues { (_, sets) -> sets.maxOf { it.weight!! } }
}

/** The most recently finished workout, or null if none has been completed yet. */
fun latestCompletedSession(sessions: List<WorkoutSession>): WorkoutSession? =
    sessions.filter { it.status == WorkoutStatus.COMPLETED && it.endTime != null }.maxByOrNull { it.endTime!! }
