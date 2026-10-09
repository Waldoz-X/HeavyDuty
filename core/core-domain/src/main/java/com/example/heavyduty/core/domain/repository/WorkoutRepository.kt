package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.ExerciseSetSummary
import com.example.heavyduty.core.domain.model.TrainingTotals
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.bestWeightsBefore
import com.example.heavyduty.core.domain.model.exerciseSetSummaries
import com.example.heavyduty.core.domain.model.lastHardSetAtByExercise
import com.example.heavyduty.core.domain.model.latestCompletedSession
import com.example.heavyduty.core.domain.model.trainingTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository contract for workout session and set operations.
 */
interface WorkoutRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    fun getSessionById(id: Long): Flow<WorkoutSession?>
    fun getSessionWithSets(id: Long): Flow<WorkoutSession?>
    fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>>
    /** Completed sessions for one routine, oldest first, each with its own sets already loaded. */
    fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>>
    fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>>
    fun getAllCompletedSets(): Flow<List<WorkoutSet>>
    /** Working and warm-up sets of finished workouts that started in the range (like [getSessionsByDateRange]). */
    fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>>
    fun getActiveSession(): Flow<WorkoutSession?>
    suspend fun insertSession(session: WorkoutSession): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(session: WorkoutSession)
    suspend fun insertSet(set: WorkoutSet): Long
    suspend fun updateSet(set: WorkoutSet)
    suspend fun deleteSet(set: WorkoutSet)

    /*
     * Summaries over all finished training. The defaults compute them from the full lists and
     * define the expected result; the database implementation answers them with aggregate
     * queries instead, so years of sets are never loaded just to show a few numbers.
     */

    /** Per-exercise working-set count and best estimated 1RM — see [exerciseSetSummaries]. */
    fun getExerciseSetSummaries(): Flow<List<ExerciseSetSummary>> =
        getAllCompletedSets().map(::exerciseSetSummaries)

    /** When each exercise last had a hard set — see [lastHardSetAtByExercise]. */
    fun getLastHardSetTimes(): Flow<Map<Long, Long>> =
        getAllCompletedSets().map(::lastHardSetAtByExercise)

    /** Workouts, time, volume and working sets of all finished training — see [trainingTotals]. */
    fun getAllTimeTotals(): Flow<TrainingTotals> =
        combine(getAllSessions(), getAllCompletedSets(), ::trainingTotals)

    /** See [latestCompletedSession]. */
    fun getLatestCompletedSession(): Flow<WorkoutSession?> =
        getAllSessions().map(::latestCompletedSession)

    /** See [bestWeightsBefore]. */
    fun getBestWeightsBefore(time: Long): Flow<Map<Long, Float>> =
        combine(getAllSessions(), getAllCompletedSets()) { sessions, sets -> bestWeightsBefore(sessions, sets, time) }
}
