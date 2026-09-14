package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Data class for aggregated workout statistics.
 */
data class WorkoutStats(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val cancelledSessions: Int = 0,
    val totalDurationMinutes: Int = 0,
    val averageDurationMinutes: Int = 0,
)

/**
 * Use case to calculate overall workout statistics.
 */
class GetStatsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(): Flow<WorkoutStats> =
        workoutRepository.getAllSessions().map { sessions ->
            val completed = sessions.filter { it.status == WorkoutStatus.COMPLETED }
            val cancelled = sessions.filter { it.status == WorkoutStatus.CANCELLED }
            val totalMinutes = completed.sumOf { it.totalDurationSecs } / 60
            WorkoutStats(
                totalSessions = completed.size + cancelled.size,
                completedSessions = completed.size,
                cancelledSessions = cancelled.size,
                totalDurationMinutes = totalMinutes,
                averageDurationMinutes = if (completed.isNotEmpty()) totalMinutes / completed.size else 0,
            )
        }
}
