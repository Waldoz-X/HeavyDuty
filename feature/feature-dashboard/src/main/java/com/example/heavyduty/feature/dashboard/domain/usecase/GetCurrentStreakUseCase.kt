package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.currentStreakDays
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Consecutive days, ending today, with a completed workout — see [currentStreakDays]. */
class GetCurrentStreakUseCase @Inject constructor(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<Int> = trainingHistoryRepository.getAllSessions().map { sessions ->
        currentStreakDays(sessions, clock.nowMillis())
    }
}
