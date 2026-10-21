package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

/** The training log as lists: workouts, and the sets that count, by period, routine or exercise. */
interface TrainingHistoryRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>>

    /** Completed sessions for one routine, oldest first, each with its own sets already loaded. */
    fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>>
    fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>>
    fun getAllCompletedSets(): Flow<List<WorkoutSet>>

    /** One exercise's sets in finished workouts that started before [beforeStartTime]: what a workout's records beat. */
    fun getCompletedSetsByExerciseIdBefore(exerciseId: Long, beforeStartTime: Long): Flow<List<WorkoutSet>>

    /** Working and warm-up sets of finished workouts that started in the range (like [getSessionsByDateRange]). */
    fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>>
}
