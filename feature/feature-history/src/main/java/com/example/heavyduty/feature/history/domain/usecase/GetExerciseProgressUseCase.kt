package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to retrieve all non-warmup sets for a specific exercise, ordered by date.
 * Used for generating progress charts — a warmup set would show up as an artificial dip
 * in the trend line, so it's excluded here rather than at the chart-building call site.
 */
class GetExerciseProgressUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<List<WorkoutSet>> =
        workoutRepository.getSetsByExerciseId(exerciseId).map { it.excludeWarmups() }
}
