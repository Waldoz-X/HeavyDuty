package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * Consecutive days (ending today) with at least one completed workout. A missed "today"
 * doesn't break a streak that was active as of yesterday. Same algorithm as
 * ProfileViewModel.calculateCurrentStreakDays, duplicated here per this codebase's
 * cross-feature-module convention (feature modules never depend on each other).
 */
class GetCurrentStreakUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(): Flow<Int> =
        workoutRepository.getAllSessions().map { sessions -> calculateCurrentStreakDays(sessions) }

    private fun calculateCurrentStreakDays(sessions: List<WorkoutSession>): Int {
        val trainedDays = sessions
            .filter { it.endTime != null }
            .map { dayStartMillis(it.startTime) }
            .toSet()
        if (trainedDays.isEmpty()) return 0

        val cursor = Calendar.getInstance()
        if (!trainedDays.contains(dayStartMillis(cursor.timeInMillis))) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        while (trainedDays.contains(dayStartMillis(cursor.timeInMillis))) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun dayStartMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
