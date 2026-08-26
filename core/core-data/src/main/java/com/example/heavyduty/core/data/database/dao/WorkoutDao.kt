package com.example.heavyduty.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import com.example.heavyduty.core.data.database.relation.SessionWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Session operations
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<WorkoutSessionEntity?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getSessionWithSets(id: Long): Flow<SessionWithSets?>

    @Query("SELECT * FROM workout_sessions WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Delete
    suspend fun deleteSession(session: WorkoutSessionEntity)

    // Set operations
    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY completedAt DESC")
    fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE isCompleted = 1")
    fun getAllCompletedSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE isCompleted = 1 AND completedAt BETWEEN :startTime AND :endTime")
    fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)
}
