package com.example.heavyduty.feature.stats.domain.model

/**
 * How often an exercise has been performed, used to rank "most used" exercises.
 */
data class ExerciseFrequency(
    val exerciseId: Long,
    val exerciseName: String,
    val setCount: Int,
    val imageUrl: String = "",
)
