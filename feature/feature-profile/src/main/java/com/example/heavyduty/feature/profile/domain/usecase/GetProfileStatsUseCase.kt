package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.profile.domain.model.ProfileStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case that aggregates workout history into the stats shown on the profile screen.
 */
class GetProfileStatsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(): Flow<ProfileStats> = combine(
        workoutRepository.getAllSessions(),
        workoutRepository.getAllCompletedSets(),
    ) { sessions, rawSets ->
        val sets = rawSets.excludeWarmups()
        ProfileStats(
            totalWorkouts = sessions.count { it.status == WorkoutStatus.COMPLETED },
            totalSets = sets.size,
            totalVolumeKg = sets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }.toFloat(),
            memberSinceMillis = sessions.minOfOrNull { it.startTime },
        )
    }
}
