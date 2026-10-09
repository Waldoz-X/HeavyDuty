package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.heavyduty.core.domain.model.currentStreakDays
import javax.inject.Inject

/** Consecutive days, ending today, with a completed workout — see [currentStreakDays]. */
class GetCurrentStreakUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(): Flow<Int> =
        workoutRepository.getAllSessions().map { sessions -> currentStreakDays(sessions) }
}
