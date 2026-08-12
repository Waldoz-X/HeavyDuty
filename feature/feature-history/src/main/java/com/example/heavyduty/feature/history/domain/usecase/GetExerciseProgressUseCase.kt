package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve all sets for a specific exercise, ordered by date.
 * Used for generating progress charts.
 */
class GetExerciseProgressUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<List<WorkoutSet>> =
        workoutRepository.getSetsByExerciseId(exerciseId)
}
