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

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE routineId = :routineId AND status = 'COMPLETED' ORDER BY startTime ASC")
    fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<SessionWithSets>>

    // ORDER BY makes this deterministic if more than one session is ever left IN_PROGRESS
    // (e.g. a duplicate created by a double-tap race) so resume always picks the newest one.
    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY startTime DESC LIMIT 1")
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

    /**
     * Logged sets of FINISHED workouts only — what every stat, record and total counts, so an
     * unfinished or abandoned workout never inflates them (and all screens agree).
     */
    @Query(
        "SELECT s.* FROM workout_sets s INNER JOIN workout_sessions w ON s.sessionId = w.id " +
            "WHERE s.isCompleted = 1 AND w.status = 'COMPLETED'",
    )
    fun getAllCompletedSets(): Flow<List<WorkoutSetEntity>>

    /**
     * Sets of finished workouts that *started* in the range, matching [getSessionsByDateRange]: a
     * workout that runs past midnight at the end of a period counts whole in the period it began,
     * instead of its workout landing in one period and its later sets in the next (where they
     * were then dropped for not belonging to any of that period's workouts).
     */
    @Query(
        "SELECT s.* FROM workout_sets s INNER JOIN workout_sessions w ON s.sessionId = w.id " +
            "WHERE s.isCompleted = 1 AND w.status = 'COMPLETED' AND w.startTime BETWEEN :startTime AND :endTime",
    )
    fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    /** Every logged set, finished workout or not — for backups, which must not drop any. */
    @Query("SELECT * FROM workout_sets WHERE isCompleted = 1")
    fun getAllLoggedSets(): Flow<List<WorkoutSetEntity>>
}
