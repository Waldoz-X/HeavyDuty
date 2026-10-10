package com.example.heavyduty.feature.stats.presentation.mapaactividad

import com.example.heavyduty.feature.stats.domain.usecase.DayTotals
import java.util.Calendar

/** How the frequency card groups the history: one week of days, one month of days, or a year of months. */
enum class FrequencyView { WEEK, MONTH, YEAR }

/** One day in the week or month view. */
data class FrequencyDay(
    /** Local midnight. */
    val dayStart: Long,
    val dayOfMonth: Int,
    val sessions: Int,
    val minutes: Float,
    /** 0 for a rest day; a trained day starts at [MIN_TRAINED_INTENSITY] and reaches 1 on the period's longest day. */
    val intensity: Float,
    val isToday: Boolean,
    val isFuture: Boolean
)

/** One month in the year view. */
data class FrequencyMonth(
    /** Local midnight of the 1st. */
    val monthStart: Long,
    val trainedDays: Int,
    val sessions: Int,
    val minutes: Float,
    val isCurrent: Boolean,
    val isFuture: Boolean
)

sealed interface FrequencyContent {
    data class Week(val days: List<FrequencyDay>) : FrequencyContent

    /** Monday-first rows of 7; null pads the days before the 1st and after the last day. */
    data class Month(val weeks: List<List<FrequencyDay?>>) : FrequencyContent

    data class Year(val months: List<FrequencyMonth>) : FrequencyContent
}

/** Everything the frequency card shows for the selected view and period. */
data class FrequencyPanel(
    val view: FrequencyView,
    /** Local midnight the period starts at; its end is exclusive. */
    val periodStart: Long,
    val periodEnd: Long,
    val sessions: Int,
    val minutes: Float,
    val trainedDays: Int,
    /** There is history before this period. */
    val canGoBack: Boolean,
    /** This isn't the current period (the future holds nothing to show). */
    val canGoForward: Boolean,
    val content: FrequencyContent
)

/**
 * The lightest shade a day with any workout gets. Scaling straight by the longest day drew an
 * hour beside a forgotten 35-hour session at 0.03 — the same as resting.
 */
internal const val MIN_TRAINED_INTENSITY = 0.25f

private const val DAYS_IN_WEEK = 7
private const val MONTHS_IN_YEAR = 12

/**
 * The [view] of the period [offset] steps from the current one (0 = this week/month/year,
 * -1 = the previous one…), built from [dailyTotals] keyed by local midnight. Weeks start on Monday.
 */
fun buildFrequencyPanel(view: FrequencyView, offset: Int, now: Long, dailyTotals: Map<Long, DayTotals>): FrequencyPanel {
    val today = startOfDay(now)
    val calendar = periodStartCalendar(view, today, offset)
    val periodStart = calendar.timeInMillis
    calendar.add(view.calendarUnit, 1)
    val periodEnd = calendar.timeInMillis

    val inPeriod = dailyTotals.filterKeys { it in periodStart until periodEnd }
    val content = when (view) {
        FrequencyView.WEEK -> FrequencyContent.Week(days(periodStart, DAYS_IN_WEEK, today, inPeriod))
        FrequencyView.MONTH -> FrequencyContent.Month(monthWeeks(periodStart, today, inPeriod))
        FrequencyView.YEAR -> FrequencyContent.Year(months(periodStart, today, inPeriod))
    }
    return FrequencyPanel(
        view = view,
        periodStart = periodStart,
        periodEnd = periodEnd,
        sessions = inPeriod.values.sumOf { it.sessions },
        minutes = inPeriod.values.sumOf { it.minutes.toDouble() }.toFloat(),
        trainedDays = inPeriod.size,
        canGoBack = dailyTotals.keys.any { it < periodStart },
        canGoForward = offset < 0,
        content = content
    )
}

private val FrequencyView.calendarUnit: Int
    get() = when (this) {
        FrequencyView.WEEK -> Calendar.WEEK_OF_YEAR
        FrequencyView.MONTH -> Calendar.MONTH
        FrequencyView.YEAR -> Calendar.YEAR
    }

private fun periodStartCalendar(view: FrequencyView, today: Long, offset: Int): Calendar = Calendar.getInstance().apply {
    timeInMillis = today
    when (view) {
        FrequencyView.WEEK -> add(Calendar.DAY_OF_YEAR, -(isoDayOfWeek(this) - 1))
        FrequencyView.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
        FrequencyView.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
    }
    add(view.calendarUnit, offset)
}

private fun days(from: Long, count: Int, today: Long, totals: Map<Long, DayTotals>): List<FrequencyDay> {
    val longest = totals.values.maxOfOrNull { it.minutes } ?: 0f
    val calendar = Calendar.getInstance().apply { timeInMillis = from }
    return List(count) {
        val day = calendar.timeInMillis
        val dayTotals = totals[day]
        FrequencyDay(
            dayStart = day,
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
            sessions = dayTotals?.sessions ?: 0,
            minutes = dayTotals?.minutes ?: 0f,
            intensity = intensity(dayTotals, longest),
            isToday = day == today,
            isFuture = day > today
        ).also { calendar.add(Calendar.DAY_OF_YEAR, 1) }
    }
}

private fun monthWeeks(monthStart: Long, today: Long, totals: Map<Long, DayTotals>): List<List<FrequencyDay?>> {
    val calendar = Calendar.getInstance().apply { timeInMillis = monthStart }
    val leading = isoDayOfWeek(calendar) - 1
    val dayCount = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells: List<FrequencyDay?> = List(leading) { null } + days(monthStart, dayCount, today, totals)
    return cells.chunked(DAYS_IN_WEEK).map { week -> week + List(DAYS_IN_WEEK - week.size) { null } }
}

private fun months(yearStart: Long, today: Long, totals: Map<Long, DayTotals>): List<FrequencyMonth> {
    val calendar = Calendar.getInstance().apply { timeInMillis = yearStart }
    return List(MONTHS_IN_YEAR) {
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis
        val month = totals.filterKeys { it in start until end }.values
        FrequencyMonth(
            monthStart = start,
            trainedDays = month.size,
            sessions = month.sumOf { it.sessions },
            minutes = month.sumOf { it.minutes.toDouble() }.toFloat(),
            isCurrent = today in start until end,
            isFuture = start > today
        )
    }
}

private fun intensity(day: DayTotals?, longestMinutes: Float): Float = when {
    day == null || day.sessions == 0 -> 0f
    longestMinutes <= 0f -> 1f
    else -> MIN_TRAINED_INTENSITY + (1f - MIN_TRAINED_INTENSITY) * (day.minutes / longestMinutes).coerceIn(0f, 1f)
}

/** Monday = 1 … Sunday = 7. */
private fun isoDayOfWeek(calendar: Calendar): Int = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + DAYS_IN_WEEK) % DAYS_IN_WEEK + 1

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
