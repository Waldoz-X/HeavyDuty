package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject

private const val HEATMAP_WEEKS = 53

/**
 * Minutes trained per day (keyed by local-midnight millis) over the last ~53 weeks,
 * normalized 0..1 against the busiest day in that window — feeds HdActivityHeatmap's
 * GitHub-style year grid. Days with no completed session simply have no entry.
 */
class GetTrainingActivityHeatmapUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(): Flow<Map<Long, Float>> =
        workoutRepository.getAllSessions().map { sessions ->
            val calendar = Calendar.getInstance()
            
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -(HEATMAP_WEEKS * 7 - 1))
            val windowStart = calendar.timeInMillis

            val minutesByDay = mutableMapOf<Long, Float>()
            sessions
                .filter { it.endTime != null && it.startTime >= windowStart }
                .forEach { session ->
                    calendar.timeInMillis = session.startTime
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val day = calendar.timeInMillis
                    minutesByDay[day] = (minutesByDay[day] ?: 0f) + session.totalDurationSecs / 60f
                }

            val maxMinutes = minutesByDay.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            minutesByDay.mapValues { (_, minutes) -> (minutes / maxMinutes).coerceIn(0f, 1f) }
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
