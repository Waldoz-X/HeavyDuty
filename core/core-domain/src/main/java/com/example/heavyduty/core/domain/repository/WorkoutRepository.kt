package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

/**
 * One workout at a time: the session in progress, a session being edited, and writing their
 * sets. What all the training adds up to is read through [TrainingHistoryRepository] and
 * [TrainingStatsRepository].
 */
interface WorkoutRepository {
    fun getSessionById(id: Long): Flow<WorkoutSession?>
    fun getSessionWithSets(id: Long): Flow<WorkoutSession?>
    fun getActiveSession(): Flow<WorkoutSession?>
    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)

    /**
     * Stores [set] at its number. An exercise's sets in a workout are kept numbered 1, 2, 3…:
     * inserting at a taken number (putting a deleted set back) moves the later ones up one.
     */
    suspend fun insertSet(set: WorkoutSet): Long
    suspend fun updateSet(set: WorkoutSet)

    /** Deletes [set]; the exercise's later sets in that workout move down one, leaving no gap. */
    suspend fun deleteSet(set: WorkoutSet)
}
