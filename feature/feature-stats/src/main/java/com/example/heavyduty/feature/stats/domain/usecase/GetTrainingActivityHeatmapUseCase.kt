package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject

data class DayOfWeekStat(
    val dayOfWeek: Int,
    val dayName: String,
    val shortName: String,
    val count: Int,
    val percentage: Float,
)

data class TimeSlotStat(
    val slotName: String,
    val timeRange: String,
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
    val favoriteDayName: String? = null,
    val timeSlotDistribution: List<TimeSlotStat> = emptyList(),
    val favoriteTimeSlotName: String? = null,
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
) {
    /** [now] is the clock the streaks are measured against; tests pin it to a fixed date. */
    operator fun invoke(now: () -> Long = System::currentTimeMillis): Flow<ActivityHeatmapStatsReport> =
        workoutRepository.getAllSessions().map { sessions ->
            val nowMillis = now()
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
            val sessionsByTimeSlot = mutableMapOf<String, Int>()

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
                val slot = when (hour) {
                    in 5..11 -> "Mañana"
                    in 12..17 -> "Tarde"
                    in 18..23 -> "Noche"
                    else -> "Madrugada"
                }
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
                Calendar.MONDAY to ("Lunes" to "Lun"),
                Calendar.TUESDAY to ("Martes" to "Mar"),
                Calendar.WEDNESDAY to ("Miércoles" to "Mié"),
                Calendar.THURSDAY to ("Jueves" to "Jue"),
                Calendar.FRIDAY to ("Viernes" to "Vie"),
                Calendar.SATURDAY to ("Sábado" to "Sáb"),
                Calendar.SUNDAY to ("Domingo" to "Dom"),
            )

            val totalSessions = completedSessions.size
            val dayStats = dayOrder.map { (calDay, names) ->
                val count = sessionsByDayOfWeek[calDay] ?: 0
                val pct = if (totalSessions > 0) (count.toFloat() / totalSessions.toFloat() * 100f) else 0f
                DayOfWeekStat(
                    dayOfWeek = calDay,
                    dayName = names.first,
                    shortName = names.second,
                    count = count,
                    percentage = pct,
                )
            }
            val favoriteDay = dayStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.dayName

            // Patrón por franja horaria
            val slotOrder = listOf(
                "Mañana" to "05:00 - 12:00",
                "Tarde" to "12:00 - 18:00",
                "Noche" to "18:00 - 24:00",
                "Madrugada" to "00:00 - 05:00",
            )
            val slotStats = slotOrder.map { (slotName, range) ->
                val count = sessionsByTimeSlot[slotName] ?: 0
                val pct = if (totalSessions > 0) (count.toFloat() / totalSessions.toFloat() * 100f) else 0f
                TimeSlotStat(
                    slotName = slotName,
                    timeRange = range,
                    count = count,
                    percentage = pct,
                )
            }
            val favoriteSlot = slotStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.slotName

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
                favoriteDayName = favoriteDay,
                timeSlotDistribution = slotStats,
                favoriteTimeSlotName = favoriteSlot,
            )
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
