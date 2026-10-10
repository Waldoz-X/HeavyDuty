package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The most recently finished workout, or null if none has been completed yet. */
class GetRecentWorkoutUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    operator fun invoke(): Flow<WorkoutSession?> = workoutRepository.getLatestCompletedSession().distinctUntilChanged()
}
