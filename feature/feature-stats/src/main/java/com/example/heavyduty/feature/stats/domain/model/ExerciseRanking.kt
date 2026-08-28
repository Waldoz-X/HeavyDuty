package com.example.heavyduty.feature.stats.domain.model

/**
 * An exercise ranked by its estimated one-rep max (see
 * [com.example.heavyduty.core.domain.model.estimatedOneRepMax]) — a local stand-in for
 * "ranking eligible" exercises, since this app has no social backend to compare against
 * other users. [sourceWeightKg]/[sourceReps] are the actual logged set the estimate came
 * from, not necessarily the heaviest weight ever lifted for this exercise.
 */
data class ExerciseRanking(
    val exerciseId: Long,
    val exerciseName: String,
    val estimatedOneRepMaxKg: Float,
    val sourceWeightKg: Float,
    val sourceReps: Int,
    val achievedAtMillis: Long?,
)
