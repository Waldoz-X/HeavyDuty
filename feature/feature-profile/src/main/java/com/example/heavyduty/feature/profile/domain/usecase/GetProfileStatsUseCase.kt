package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.profile.domain.model.ProfileStats
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case that aggregates workout history into the stats shown on the profile screen.
 */
class GetProfileStatsUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    operator fun invoke(): Flow<ProfileStats> = combine(
        workoutRepository.getAllTimeTotals(),
        workoutRepository.getAllSessions()
    ) { totals, sessions ->
        ProfileStats(
            totalWorkouts = totals.workouts,
            totalSets = totals.sets,
            totalVolumeKg = totals.volumeKg,
            memberSinceMillis = sessions.minOfOrNull { it.startTime }
        )
    }
}
