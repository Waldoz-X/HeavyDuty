package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus

/*
 * Shared data for the stats use-case tests; the fake repositories come from core-testing.
 */

const val HOUR = 3_600_000L
const val DAY = 24 * HOUR

/** A compound lift: most of each set goes to chest, the rest to triceps and shoulders. */
val BENCH = Exercise(
    id = 1, name = "Press de banca", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH,
    muscleWeights = mapOf(MuscleGroup.CHEST to 0.6f, MuscleGroup.TRICEPS to 0.25f, MuscleGroup.SHOULDERS to 0.15f),
)
val SQUAT = Exercise(id = 2, name = "Sentadilla", muscleGroup = MuscleGroup.QUADRICEPS, exerciseType = ExerciseType.STRENGTH)
val CURL = Exercise(id = 3, name = "Curl de bíceps", muscleGroup = MuscleGroup.BICEPS, exerciseType = ExerciseType.STRENGTH)
val EXERCISES = listOf(BENCH, SQUAT, CURL)

/** An exercise id with no row — sets of a deleted exercise. */
const val DELETED_EXERCISE_ID = 99L

fun session(id: Long, start: Long, durationMins: Int = 60, status: WorkoutStatus = WorkoutStatus.COMPLETED) =
    WorkoutSession(id = id, startTime = start, totalDurationSecs = durationMins * 60, status = status)

fun set(
    session: Long,
    exercise: Exercise,
    reps: Int,
    weightKg: Float,
    at: Long,
    warmup: Boolean = false,
    completed: Boolean = true,
    unit: WeightUnit? = null,
) = WorkoutSet(
    sessionId = session, exerciseId = exercise.id, reps = reps, weight = weightKg,
    isCompleted = completed, completedAt = if (completed) at else null, isWarmup = warmup, weightUnit = unit,
)
