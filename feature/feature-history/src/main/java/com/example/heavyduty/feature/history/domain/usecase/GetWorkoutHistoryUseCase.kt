package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve all workout sessions ordered by most recent.
 */
class GetWorkoutHistoryUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    operator fun invoke(): Flow<List<WorkoutSession>> = workoutRepository.getAllSessions()
}
