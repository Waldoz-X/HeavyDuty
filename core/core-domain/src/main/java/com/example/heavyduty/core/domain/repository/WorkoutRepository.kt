package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for workout session and set operations.
 */
interface WorkoutRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    fun getSessionById(id: Long): Flow<WorkoutSession?>
    fun getSessionWithSets(id: Long): Flow<WorkoutSession?>
    fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>>
    fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>>
    fun getAllCompletedSets(): Flow<List<WorkoutSet>>
    fun getActiveSession(): Flow<WorkoutSession?>
    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)
    suspend fun insertSet(set: WorkoutSet): Long
    suspend fun updateSet(set: WorkoutSet)
    suspend fun deleteSet(set: WorkoutSet)
}
