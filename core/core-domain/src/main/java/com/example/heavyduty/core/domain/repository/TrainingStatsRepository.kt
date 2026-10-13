package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.ExerciseSetSummary
import com.example.heavyduty.core.domain.model.TrainingTotals
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.bestWeightsBefore
import com.example.heavyduty.core.domain.model.exerciseSetSummaries
import com.example.heavyduty.core.domain.model.lastHardSetAtByExercise
import com.example.heavyduty.core.domain.model.latestCompletedSession
import com.example.heavyduty.core.domain.model.trainingTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Summaries over all finished training. The database answers them with aggregate queries, so
 * years of sets are never loaded just to show a few numbers; [TrainingStatsFromHistory]
 * computes them from the full lists and defines the expected result.
 */
interface TrainingStatsRepository {
    /** Per-exercise working-set count and best estimated 1RM — see [exerciseSetSummaries]. */
    fun getExerciseSetSummaries(): Flow<List<ExerciseSetSummary>>

    /** When each exercise last had a hard set — see [lastHardSetAtByExercise]. */
    fun getLastHardSetTimes(): Flow<Map<Long, Long>>

    /** Workouts, time, volume and working sets of all finished training — see [trainingTotals]. */
    fun getAllTimeTotals(): Flow<TrainingTotals>

    /** See [latestCompletedSession]. */
    fun getLatestCompletedSession(): Flow<WorkoutSession?>

    /** See [bestWeightsBefore]. */
    fun getBestWeightsBefore(time: Long): Flow<Map<Long, Float>>
}

/** The stats worked out from the whole history: the reference the database is tested against, and what in-memory fakes use. */
class TrainingStatsFromHistory(private val history: TrainingHistoryRepository) : TrainingStatsRepository {
    override fun getExerciseSetSummaries(): Flow<List<ExerciseSetSummary>> = history.getAllCompletedSets().map(::exerciseSetSummaries)

    override fun getLastHardSetTimes(): Flow<Map<Long, Long>> = history.getAllCompletedSets().map(::lastHardSetAtByExercise)

    override fun getAllTimeTotals(): Flow<TrainingTotals> =
        combine(history.getAllSessions(), history.getAllCompletedSets(), ::trainingTotals)

    override fun getLatestCompletedSession(): Flow<WorkoutSession?> = history.getAllSessions().map(::latestCompletedSession)

    override fun getBestWeightsBefore(time: Long): Flow<Map<Long, Float>> =
        combine(history.getAllSessions(), history.getAllCompletedSets()) { sessions, sets -> bestWeightsBefore(sessions, sets, time) }
}
