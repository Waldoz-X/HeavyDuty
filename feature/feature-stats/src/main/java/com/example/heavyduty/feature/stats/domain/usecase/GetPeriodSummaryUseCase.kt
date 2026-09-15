package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Workouts/duration/volume/sets for [currentRange], paired with the same totals for
 * [previousRange] so callers can show "vs. previous period" deltas — see [PeriodSummary].
 */
class GetPeriodSummaryUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(currentRange: LongRange, previousRange: LongRange): Flow<PeriodSummary> =
        combine(
            workoutRepository.getSessionsByDateRange(currentRange.first, currentRange.last),
            workoutRepository.getCompletedSetsByDateRange(currentRange.first, currentRange.last),
            workoutRepository.getSessionsByDateRange(previousRange.first, previousRange.last),
            workoutRepository.getCompletedSetsByDateRange(previousRange.first, previousRange.last),
        ) { sessions, sets, prevSessions, prevSets ->
            val current = summaryOf(sessions, sets)
            val previous = summaryOf(prevSessions, prevSets)
            current.copy(
                previousWorkouts = previous.workouts,
                previousDurationMinutes = previous.durationMinutes,
                previousVolumeKg = previous.volumeKg,
                previousSets = previous.sets,
            )
        }.distinctUntilChanged().flowOn(Dispatchers.Default)

    private fun summaryOf(sessions: List<WorkoutSession>, rawSets: List<WorkoutSet>): PeriodSummary {
        val completedSessions = sessions.filter { it.status == WorkoutStatus.COMPLETED }
        val completedSessionIds = completedSessions.map { it.id }.toSet()
        val sets = rawSets.excludeWarmups().filter { it.sessionId in completedSessionIds }
        val volumeKg = sets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }.toFloat()
        return PeriodSummary(
            workouts = completedSessions.size,
            durationMinutes = completedSessions.sumOf { it.totalDurationSecs } / 60,
            volumeKg = volumeKg,
            sets = sets.size,
        )
    }
}
