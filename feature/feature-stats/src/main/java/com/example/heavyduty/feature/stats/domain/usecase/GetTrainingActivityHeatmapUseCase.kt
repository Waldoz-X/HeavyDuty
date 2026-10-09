package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject

/** [dayOfWeek] is a java.util.Calendar day (SUNDAY = 1 … SATURDAY = 7); the UI names it. */
data class DayOfWeekStat(
    val dayOfWeek: Int,
    val count: Int,
    val percentage: Float,
)

/** Part of the day a workout started in; the UI names it in the app's language. */
enum class TimeSlot(val startHour: Int, val endHour: Int) {
    MORNING(5, 12),
    AFTERNOON(12, 18),
    EVENING(18, 24),
    NIGHT(0, 5),
    ;

    companion object {
        fun ofHour(hour: Int): TimeSlot = when (hour) {
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            in 18..23 -> EVENING
            else -> NIGHT
        }
    }
}

data class TimeSlotStat(
    val slot: TimeSlot,
    val count: Int,
    val percentage: Float,
)

data class ActivityHeatmapStatsReport(
    val activityByDay: Map<Long, Float> = emptyMap(),
    val totalDaysTrained: Int = 0,
    val totalSessions: Int = 0,
    val totalHoursTrained: Float = 0f,
    val avgMinutesPerSession: Int = 0,
    val consistencyPercentage: Float = 0f,
    val activeWeeksCount: Int = 0,
    val totalWeeksInWindow: Int = 53,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val currentStreakWeeks: Int = 0,
    val longestStreakWeeks: Int = 0,
    val dayOfWeekDistribution: List<DayOfWeekStat> = emptyList(),
    /** Calendar day of the week with the most workouts, if any. */
    val favoriteDayOfWeek: Int? = null,
    val timeSlotDistribution: List<TimeSlotStat> = emptyList(),
    val favoriteTimeSlot: TimeSlot? = null,
)

private const val HEATMAP_WEEKS = 53

/**
 * Aggregates training activity over the last ~53 weeks:
 * - Normalized intensity grid for GitHub-style heatmap.
 * - Current and longest daily streaks.
 * - Current and longest weekly streaks.
 * - Day of week frequency and time-of-day distribution.
 */
class GetTrainingActivityHeatmapUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<ActivityHeatmapStatsReport> =
        workoutRepository.getAllSessions().map { sessions ->
            val nowMillis = clock.nowMillis()
            val calendar = Calendar.getInstance()

            calendar.timeInMillis = nowMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayMidnight = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, -(HEATMAP_WEEKS * 7 - 1))
            val windowStart = calendar.timeInMillis

            val completedSessions = sessions.filter {
                it.status == WorkoutStatus.COMPLETED && it.startTime >= windowStart
            }

            val minutesByDay = mutableMapOf<Long, Float>()
            val sessionsByDayOfWeek = mutableMapOf<Int, Int>()
            val sessionsByTimeSlot = mutableMapOf<TimeSlot, Int>()

            var totalDurationSecs = 0

            completedSessions.forEach { session ->
                totalDurationSecs += session.totalDurationSecs

                calendar.timeInMillis = session.startTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val day = calendar.timeInMillis
                minutesByDay[day] = (minutesByDay[day] ?: 0f) + (session.totalDurationSecs / 60f)

                calendar.timeInMillis = session.startTime
                val dow = calendar.get(Calendar.DAY_OF_WEEK)
                sessionsByDayOfWeek[dow] = (sessionsByDayOfWeek[dow] ?: 0) + 1

                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val slot = TimeSlot.ofHour(hour)
                sessionsByTimeSlot[slot] = (sessionsByTimeSlot[slot] ?: 0) + 1
            }

            val maxMinutes = minutesByDay.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val normalizedHeatmap = minutesByDay.mapValues { (_, minutes) -> (minutes / maxMinutes).coerceIn(0f, 1f) }

            // Racha diaria
            val trainedDays = minutesByDay.keys.sorted()
            val oneDayMillis = 24 * 60 * 60 * 1000L
            var maxDailyStreak = 0
            var runningDailyStreak = 0
            var prevDayMillis: Long? = null

            trainedDays.forEach { dayMillis ->
                val prev = prevDayMillis
                if (prev == null) {
                    runningDailyStreak = 1
                } else {
                    // Rounded: across a DST change two midnights are 23 or 25 hours apart.
                    val diffDays = Math.round((dayMillis - prev).toDouble() / oneDayMillis).toInt()
                    if (diffDays == 1) {
                        runningDailyStreak++
                    } else if (diffDays > 1) {
                        runningDailyStreak = 1
                    }
                }
                if (runningDailyStreak > maxDailyStreak) {
                    maxDailyStreak = runningDailyStreak
                }
                prevDayMillis = dayMillis
            }

            calendar.timeInMillis = todayMidnight
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayMidnight = calendar.timeInMillis

            var currentDailyStreak = 0
            if (trainedDays.contains(todayMidnight) || trainedDays.contains(yesterdayMidnight)) {
                var checkDay = if (trainedDays.contains(todayMidnight)) todayMidnight else yesterdayMidnight
                while (trainedDays.contains(checkDay)) {
                    currentDailyStreak++
                    calendar.timeInMillis = checkDay
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    checkDay = calendar.timeInMillis
                }
            }

            // Racha semanal
            // Consecutive Monday-to-Sunday weeks get consecutive indexes, also across New Year —
            // "year * 53 + WEEK_OF_YEAR" broke there (Dec 29 can be week 1 of the next year).
            fun getWeekIndex(millis: Long): Long {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = millis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
                }
                return Math.round(cal.timeInMillis.toDouble() / (7 * oneDayMillis))
            }

            val weeksWithWorkouts = completedSessions.map { getWeekIndex(it.startTime) }.distinct().sorted()
            var maxWeeklyStreak = 0
            var runningWeeklyStreak = 0
            var prevWeekIdx: Long? = null

            weeksWithWorkouts.forEach { weekIdx ->
                val prevWeek = prevWeekIdx
                if (prevWeek == null) {
                    runningWeeklyStreak = 1
                } else {
                    if (weekIdx == prevWeek + 1) {
                        runningWeeklyStreak++
                    } else if (weekIdx > prevWeek + 1) {
                        runningWeeklyStreak = 1
                    }
                }
                if (runningWeeklyStreak > maxWeeklyStreak) {
                    maxWeeklyStreak = runningWeeklyStreak
                }
                prevWeekIdx = weekIdx
            }

            val currentWeekIdx = getWeekIndex(nowMillis)
            var currentWeeklyStreak = 0
            if (weeksWithWorkouts.contains(currentWeekIdx) || weeksWithWorkouts.contains(currentWeekIdx - 1)) {
                var checkWeek = if (weeksWithWorkouts.contains(currentWeekIdx)) currentWeekIdx else currentWeekIdx - 1
                while (weeksWithWorkouts.contains(checkWeek)) {
                    currentWeeklyStreak++
                    checkWeek--
                }
            }

            // Patrón por día de la semana
            val dayOrder = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
            )

            val totalSessions = completedSessions.size
            val dayStats = dayOrder.map { calDay ->
                val count = sessionsByDayOfWeek[calDay] ?: 0
                val pct = if (totalSessions > 0) (count.toFloat() / totalSessions.toFloat() * 100f) else 0f
                DayOfWeekStat(
                    dayOfWeek = calDay,
                    count = count,
                    percentage = pct,
                )
            }
            val favoriteDay = dayStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.dayOfWeek

            // Patrón por franja horaria
            val slotStats = TimeSlot.entries.map { slot ->
                val count = sessionsByTimeSlot[slot] ?: 0
                val pct = if (totalSessions > 0) (count.toFloat() / totalSessions.toFloat() * 100f) else 0f
                TimeSlotStat(
                    slot = slot,
                    count = count,
                    percentage = pct,
                )
            }
            val favoriteSlot = slotStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.slot

            val totalHours = totalDurationSecs / 3600f
            val avgDurationMins = if (totalSessions > 0) totalDurationSecs / totalSessions / 60 else 0
            val consistencyPct = (trainedDays.size.toFloat() / 365f * 100f).coerceIn(0f, 100f)

            ActivityHeatmapStatsReport(
                activityByDay = normalizedHeatmap,
                totalDaysTrained = trainedDays.size,
                totalSessions = totalSessions,
                totalHoursTrained = totalHours,
                avgMinutesPerSession = avgDurationMins,
                consistencyPercentage = consistencyPct,
                activeWeeksCount = weeksWithWorkouts.size,
                totalWeeksInWindow = HEATMAP_WEEKS,
                currentStreakDays = currentDailyStreak,
                longestStreakDays = maxDailyStreak,
                currentStreakWeeks = currentWeeklyStreak,
                longestStreakWeeks = maxWeeklyStreak,
                dayOfWeekDistribution = dayStats,
                favoriteDayOfWeek = favoriteDay,
                timeSlotDistribution = slotStats,
                favoriteTimeSlot = favoriteSlot,
            )
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
