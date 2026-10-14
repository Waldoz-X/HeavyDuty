package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** [dayOfWeek] is a java.util.Calendar day (SUNDAY = 1 … SATURDAY = 7); the UI names it. */
data class DayOfWeekStat(val dayOfWeek: Int, val count: Int, val percentage: Float)

/** Part of the day a workout started in; the UI names it in the app's language. */
enum class TimeSlot(val startHour: Int, val endHour: Int) {
    MORNING(5, 12),
    AFTERNOON(12, 18),
    EVENING(18, 24),
    NIGHT(0, 5)
    ;

    companion object {
        fun ofHour(hour: Int): TimeSlot = entries.firstOrNull { hour >= it.startHour && hour < it.endHour } ?: NIGHT
    }
}

data class TimeSlotStat(val slot: TimeSlot, val count: Int, val percentage: Float)

/** What was trained on one calendar day: finished workouts and their minutes. */
data class DayTotals(val sessions: Int, val minutes: Float)

data class ActivityHeatmapStatsReport(
    /**
     * Every day with a finished workout, keyed by its local midnight — the whole history, not
     * just the last year, so the frequency calendar can be browsed back to the first workout.
     */
    val dailyTotals: Map<Long, DayTotals> = emptyMap(),
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
    val favoriteTimeSlot: TimeSlot? = null
)

private const val HEATMAP_WEEKS = 53

/**
 * Aggregates training activity over the last ~53 weeks:
 * - Workouts and minutes per day, over the whole history, for the frequency calendar.
 * - Current and longest daily streaks.
 * - Current and longest weekly streaks.
 * - Day of week frequency and time-of-day distribution.
 */
class GetTrainingActivityHeatmapUseCase @Inject constructor(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val clock: Clock,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(): Flow<ActivityHeatmapStatsReport> = trainingHistoryRepository.getAllSessions().map { sessions ->
        val nowMillis = clock.nowMillis()
        val todayMidnight = startOfDay(nowMillis)
        val windowStart = addDays(todayMidnight, -(HEATMAP_WEEKS * DAYS_PER_WEEK - 1))
        val completed = sessions.filter { it.status == WorkoutStatus.COMPLETED }
        val inWindow = completed.filter { it.startTime >= windowStart }

        val trainedDays = inWindow.map { startOfDay(it.startTime) }.distinct().sorted()
        val weeksWithWorkouts = inWindow.map { weekIndex(it.startTime) }.distinct().sorted()
        val totalSessions = inWindow.size
        val totalDurationSecs = inWindow.sumOf { it.totalDurationSecs }
        val dayStats = dayOfWeekStats(inWindow)
        val slotStats = timeSlotStats(inWindow)

        ActivityHeatmapStatsReport(
            dailyTotals = dailyTotals(completed),
            totalDaysTrained = trainedDays.size,
            totalSessions = totalSessions,
            totalHoursTrained = totalDurationSecs / SECONDS_PER_HOUR,
            avgMinutesPerSession = if (totalSessions > 0) totalDurationSecs / totalSessions / SECONDS_PER_MINUTE.toInt() else 0,
            consistencyPercentage = (trainedDays.size.toFloat() / DAYS_PER_YEAR * PERCENT).coerceIn(0f, PERCENT),
            activeWeeksCount = weeksWithWorkouts.size,
            totalWeeksInWindow = HEATMAP_WEEKS,
            currentStreakDays = currentDailyStreak(trainedDays.toSet(), todayMidnight),
            // Rounded: across a DST change two midnights are 23 or 25 hours apart.
            longestStreakDays = longestStreak(trainedDays) { prev, day -> Math.round((day - prev).toDouble() / DAY_MILLIS) },
            currentStreakWeeks = currentWeeklyStreak(weeksWithWorkouts.toSet(), weekIndex(nowMillis)),
            longestStreakWeeks = longestStreak(weeksWithWorkouts) { prev, week -> week - prev },
            dayOfWeekDistribution = dayStats,
            favoriteDayOfWeek = dayStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.dayOfWeek,
            timeSlotDistribution = slotStats,
            favoriteTimeSlot = slotStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }?.slot
        )
    }.distinctUntilChanged().flowOn(defaultDispatcher)
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis

/**
 * Consecutive Monday-to-Sunday weeks get consecutive indexes, also across New Year —
 * "year * 53 + WEEK_OF_YEAR" broke there (Dec 29 can be week 1 of the next year).
 */
private fun weekIndex(millis: Long): Long {
    val monday = Calendar.getInstance().apply {
        timeInMillis = startOfDay(millis)
        val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + DAYS_PER_WEEK) % DAYS_PER_WEEK
        add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
    }
    return Math.round(monday.timeInMillis.toDouble() / (DAYS_PER_WEEK * DAY_MILLIS))
}

/** The longest run in [sorted] (distinct, ascending) where each item is [gap] 1 from the one before. */
private fun longestStreak(sorted: List<Long>, gap: (previous: Long, current: Long) -> Long): Int {
    var longest = 0
    var running = 0
    sorted.forEachIndexed { index, item ->
        running = if (index > 0 && gap(sorted[index - 1], item) == 1L) running + 1 else 1
        longest = maxOf(longest, running)
    }
    return longest
}

/** Days trained in a row up to today — or up to yesterday, while today can still be trained. */
private fun currentDailyStreak(trainedDays: Set<Long>, todayMidnight: Long): Int {
    var day = if (todayMidnight in trainedDays) todayMidnight else addDays(todayMidnight, -1)
    var streak = 0
    while (day in trainedDays) {
        streak++
        day = addDays(day, -1)
    }
    return streak
}

/** Weeks trained in a row up to this one — or up to last week, while this one can still be. */
private fun currentWeeklyStreak(weeks: Set<Long>, currentWeek: Long): Int {
    var week = if (currentWeek in weeks) currentWeek else currentWeek - 1
    var streak = 0
    while (week in weeks) {
        streak++
        week--
    }
    return streak
}

private val WEEK_DAYS = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY
)

private fun share(count: Int, total: Int): Float = if (total > 0) count.toFloat() / total.toFloat() * PERCENT else 0f

private fun dayOfWeekStats(sessions: List<WorkoutSession>): List<DayOfWeekStat> {
    val byDay = sessions.groupingBy { Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.DAY_OF_WEEK) }.eachCount()
    return WEEK_DAYS.map { day ->
        val count = byDay[day] ?: 0
        DayOfWeekStat(dayOfWeek = day, count = count, percentage = share(count, sessions.size))
    }
}

private fun timeSlotStats(sessions: List<WorkoutSession>): List<TimeSlotStat> {
    val bySlot = sessions.groupingBy {
        TimeSlot.ofHour(Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.HOUR_OF_DAY))
    }.eachCount()
    return TimeSlot.entries.map { slot ->
        val count = bySlot[slot] ?: 0
        TimeSlotStat(slot = slot, count = count, percentage = share(count, sessions.size))
    }
}

private fun dailyTotals(completed: List<WorkoutSession>): Map<Long, DayTotals> = completed
    .groupBy { startOfDay(it.startTime) }
    .mapValues { (_, day) -> DayTotals(sessions = day.size, minutes = day.sumOf { it.totalDurationSecs } / SECONDS_PER_MINUTE) }

private const val DAYS_PER_WEEK = 7
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val DAYS_PER_YEAR = 365f
private const val SECONDS_PER_MINUTE = 60f
private const val SECONDS_PER_HOUR = 3600f
private const val PERCENT = 100f
