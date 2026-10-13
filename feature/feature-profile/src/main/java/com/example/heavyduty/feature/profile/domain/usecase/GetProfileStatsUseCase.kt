package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.core.domain.repository.TrainingStatsRepository
import com.example.heavyduty.feature.profile.domain.model.ProfileStats
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case that aggregates workout history into the stats shown on the profile screen.
 */
class GetProfileStatsUseCase @Inject constructor(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val trainingStatsRepository: TrainingStatsRepository
) {
    operator fun invoke(): Flow<ProfileStats> = combine(
        trainingStatsRepository.getAllTimeTotals(),
        trainingHistoryRepository.getAllSessions()
    ) { totals, sessions ->
        ProfileStats(
            totalWorkouts = totals.workouts,
            totalSets = totals.sets,
            totalVolumeKg = totals.volumeKg,
            memberSinceMillis = sessions.minOfOrNull { it.startTime }
        )
    }
}
