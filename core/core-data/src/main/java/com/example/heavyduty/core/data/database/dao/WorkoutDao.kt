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
import com.example.heavyduty.core.data.database.relation.ExerciseSetSummaryRow
import com.example.heavyduty.core.data.database.relation.ExerciseWeightRow
import com.example.heavyduty.core.data.database.relation.LastHardSetRow
import com.example.heavyduty.core.data.database.relation.SessionTotalsRow
import com.example.heavyduty.core.data.database.relation.SessionWithSets
import com.example.heavyduty.core.data.database.relation.SetTotalsRow
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
            "WHERE s.isCompleted = 1 AND w.status = 'COMPLETED'"
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
            "WHERE s.isCompleted = 1 AND w.status = 'COMPLETED' AND w.startTime BETWEEN :startTime AND :endTime"
    )
    fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query(
        "UPDATE workout_sets SET setNumber = setNumber - 1 " +
            "WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setNumber > :setNumber"
    )
    suspend fun closeSetNumberGap(sessionId: Long, exerciseId: Long, setNumber: Int)

    @Query(
        "UPDATE workout_sets SET setNumber = setNumber + 1 " +
            "WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setNumber >= :setNumber AND id != :setId"
    )
    suspend fun openSetNumberGap(sessionId: Long, exerciseId: Long, setNumber: Int, setId: Long)

    /*
     * An exercise's sets in a workout stay numbered 1, 2, 3…: "Anterior" and the suggestions
     * match sets by number, so deleting set 2 of 3 used to leave 1 and 3 — and next time set 2
     * had nothing to compare with.
     */

    /** Deletes a set and moves the exercise's later sets in that workout down one. */
    @Transaction
    suspend fun deleteSetKeepingNumbers(set: WorkoutSetEntity) {
        deleteSet(set)
        closeSetNumberGap(set.sessionId, set.exerciseId, set.setNumber)
    }

    /** Inserts a set at its number, moving the exercise's sets from there on up one; logging the next set moves none. */
    @Transaction
    suspend fun insertSetKeepingNumbers(set: WorkoutSetEntity): Long {
        openSetNumberGap(set.sessionId, set.exerciseId, set.setNumber, set.id)
        return insertSet(set)
    }

    /** Every logged set, finished workout or not — for backups, which must not drop any. */
    @Query("SELECT * FROM workout_sets WHERE isCompleted = 1")
    fun getAllLoggedSets(): Flow<List<WorkoutSetEntity>>

    /*
     * Aggregates over all finished training, so screens get a few rows instead of every set
     * ever logged. Each one must agree with its reference function in core-domain's
     * ExerciseHistory.kt / TrainingTotals.kt (checked by TrainingAggregatesTest).
     */

    /**
     * Working sets per exercise and its best Epley estimate, mirroring estimatedOneRepMax: none
     * without weight or reps or above 12 reps, and a true single is the weight itself. The
     * set's other columns come from the row holding the MAX (an SQLite bare-column rule).
     */
    @Query(
        """SELECT s.exerciseId AS exerciseId, COUNT(*) AS workingSets,
             MAX(CASE WHEN s.weight > 0 AND s.reps = 1 THEN s.weight
                      WHEN s.weight > 0 AND s.reps BETWEEN 2 AND 12 THEN s.weight * (1 + s.reps / 30.0) END) AS bestOneRepMaxKg,
             s.weight AS bestSetWeightKg, s.reps AS bestSetReps, s.completedAt AS bestSetAt
           FROM workout_sets s INNER JOIN workout_sessions w ON s.sessionId = w.id
           WHERE s.isCompleted = 1 AND w.status = 'COMPLETED' AND s.isWarmup = 0
           GROUP BY s.exerciseId"""
    )
    fun getExerciseSetSummaries(): Flow<List<ExerciseSetSummaryRow>>

    /** Latest working set per exercise at or above [intensity] of its best estimated 1RM. */
    @Query(
        """WITH counted AS (
             SELECT s.* FROM workout_sets s INNER JOIN workout_sessions w ON s.sessionId = w.id
             WHERE s.isCompleted = 1 AND w.status = 'COMPLETED' AND s.isWarmup = 0
           ), best AS (
             SELECT exerciseId, MAX(CASE WHEN weight > 0 AND reps = 1 THEN weight
                                         WHEN weight > 0 AND reps BETWEEN 2 AND 12 THEN weight * (1 + reps / 30.0) END) AS oneRepMax
             FROM counted GROUP BY exerciseId
           )
           SELECT c.exerciseId AS exerciseId, MAX(c.completedAt) AS lastHardSetAt
           FROM counted c INNER JOIN best b ON b.exerciseId = c.exerciseId
           WHERE c.completedAt IS NOT NULL AND b.oneRepMax > 0 AND COALESCE(c.weight, 0) >= :intensity * b.oneRepMax
           GROUP BY c.exerciseId"""
    )
    fun getLastHardSetTimes(intensity: Float): Flow<List<LastHardSetRow>>

    @Query(
        """SELECT s.exerciseId AS exerciseId, MAX(s.weight) AS weightKg
           FROM workout_sets s INNER JOIN workout_sessions w ON s.sessionId = w.id
           WHERE s.isCompleted = 1 AND w.status = 'COMPLETED' AND s.isWarmup = 0
             AND s.weight IS NOT NULL AND s.completedAt IS NOT NULL AND w.startTime < :time
           GROUP BY s.exerciseId"""
    )
    fun getBestWeightsBefore(time: Long): Flow<List<ExerciseWeightRow>>

    @Query(
        "SELECT COUNT(*) AS workouts, COALESCE(SUM(totalDurationSecs), 0) AS durationSecs FROM workout_sessions WHERE status = 'COMPLETED'"
    )
    fun getCompletedSessionTotals(): Flow<SessionTotalsRow>

    @Query(
        """SELECT COUNT(*) AS sets, COALESCE(SUM(COALESCE(s.reps, 0) * COALESCE(s.weight, 0)), 0) AS volumeKg
           FROM workout_sets s INNER JOIN workout_sessions w ON s.sessionId = w.id
           WHERE s.isCompleted = 1 AND w.status = 'COMPLETED' AND s.isWarmup = 0"""
    )
    fun getWorkingSetTotals(): Flow<SetTotalsRow>

    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' AND endTime IS NOT NULL ORDER BY endTime DESC LIMIT 1")
    fun getLatestCompletedSession(): Flow<WorkoutSessionEntity?>
}
