package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve all workout sessions ordered by most recent.
 */
class GetWorkoutHistoryUseCase @Inject constructor(private val trainingHistoryRepository: TrainingHistoryRepository) {
    operator fun invoke(): Flow<List<WorkoutSession>> = trainingHistoryRepository.getAllSessions()
}
