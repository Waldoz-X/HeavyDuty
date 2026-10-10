package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.TrainingTotals
import com.example.heavyduty.core.domain.model.trainingTotals
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Completed workouts, time and volume for the dashboard's week — same rules as the stats
 * screens (see [trainingTotals]) so both show the same numbers for the same week.
 */
class GetWeeklySummaryUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    operator fun invoke(range: LongRange): Flow<TrainingTotals> = combine(
        workoutRepository.getSessionsByDateRange(range.first, range.last),
        workoutRepository.getCompletedSetsByDateRange(range.first, range.last)
    ) { sessions, sets -> trainingTotals(sessions, sets) }
        .distinctUntilChanged()
}
