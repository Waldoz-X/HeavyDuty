package com.example.heavyduty.core.domain.model

/**
 * Domain model representing an exercise in the catalog.
 */
data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val instructions: String = "",
    val imageUrl: String = "",
    val muscleGroup: MuscleGroup,
    val exerciseType: ExerciseType,
    val equipment: String = "",
    val isCustom: Boolean = false,
)
