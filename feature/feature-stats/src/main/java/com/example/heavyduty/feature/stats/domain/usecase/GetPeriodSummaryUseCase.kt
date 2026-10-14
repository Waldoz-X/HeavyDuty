package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.trainingTotals
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * Workouts/duration/volume/sets for the current range, paired with the same totals for the
 * previous range so callers can show "vs. previous period" deltas — see [PeriodSummary].
 */
class GetPeriodSummaryUseCase @Inject constructor(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(currentRange: LongRange, previousRange: LongRange): Flow<PeriodSummary> = combine(
        trainingHistoryRepository.getSessionsByDateRange(currentRange.first, currentRange.last),
        trainingHistoryRepository.getCompletedSetsByDateRange(currentRange.first, currentRange.last),
        trainingHistoryRepository.getSessionsByDateRange(previousRange.first, previousRange.last),
        trainingHistoryRepository.getCompletedSetsByDateRange(previousRange.first, previousRange.last)
    ) { sessions, sets, prevSessions, prevSets ->
        val current = summaryOf(sessions, sets)
        val previous = summaryOf(prevSessions, prevSets)
        current.copy(
            previousWorkouts = previous.workouts,
            previousDurationMinutes = previous.durationMinutes,
            previousVolumeKg = previous.volumeKg,
            previousSets = previous.sets
        )
    }.distinctUntilChanged().flowOn(defaultDispatcher)

    private fun summaryOf(sessions: List<WorkoutSession>, rawSets: List<WorkoutSet>): PeriodSummary {
        val totals = trainingTotals(sessions, rawSets)
        return PeriodSummary(
            workouts = totals.workouts,
            durationMinutes = totals.durationSecs / 60,
            volumeKg = totals.volumeKg,
            sets = totals.sets
        )
    }
}
