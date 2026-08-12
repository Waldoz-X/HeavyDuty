package com.example.heavyduty.core.domain.model

/**
 * Domain model representing a training routine.
 */
data class Routine(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: WorkoutType = WorkoutType.GYM,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val exercises: List<RoutineExercise> = emptyList(),
)

/**
 * Domain model representing an exercise within a routine,
 * including its configuration (target sets, reps, weight, etc.).
 */
data class RoutineExercise(
    val id: Long = 0,
    val routineId: Long = 0,
    val exerciseId: Long = 0,
    val exercise: Exercise? = null,
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeight: Float = 0f,
    val targetDurationSecs: Int = 0,
    val restBetweenSetsSecs: Int = 60,
    val notes: String = "",
)
