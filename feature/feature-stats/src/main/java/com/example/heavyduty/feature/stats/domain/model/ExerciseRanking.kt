package com.example.heavyduty.feature.stats.domain.model

/**
 * An exercise's best recorded set, used to rank "eligible for ranking" exercises
 * (i.e. exercises with a registered personal record).
 */
data class ExerciseRanking(
    val exerciseId: Long,
    val exerciseName: String,
    val maxWeightKg: Float,
    val repsAtMaxWeight: Int?,
    val achievedAtMillis: Long?,
)
