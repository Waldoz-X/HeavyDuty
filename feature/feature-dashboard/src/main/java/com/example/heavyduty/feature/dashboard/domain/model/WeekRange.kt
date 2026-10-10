package com.example.heavyduty.feature.dashboard.domain.model

import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * The Monday-to-Sunday week containing [timeMillis] (same weeks as the dashboard's week strip
 * and the calendar template): from Monday 00:00 up to the last millisecond before the next
 * Monday. Covering the full week (not "until now") keeps queries using it valid for sessions
 * and sets logged after the range was computed. Uses [Calendar] rather than java.time, which
 * needs API 26 without core library desugaring (minSdk is 24).
 */
fun weekRangeContaining(timeMillis: Long): LongRange {
    val calendar = startOfDay(timeMillis)
    val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    return start until calendar.timeInMillis
}

/**
 * Emits the current week's range, then re-emits when the week rolls over so screens that
 * stay alive across a week boundary move on to the new week.
 */
fun currentWeekRanges(now: () -> Long = System::currentTimeMillis): Flow<LongRange> = flow {
    while (true) {
        val range = weekRangeContaining(now())
        emit(range)
        delay((range.last + 1 - now()).coerceAtLeast(1))
    }
}

/** Calendar days from [from] to [to] (0 = same day, 1 = the day before, …), ignoring time of day. */
fun calendarDaysBetween(from: Long, to: Long): Int {
    val start = startOfDay(from).timeInMillis
    val end = startOfDay(to).timeInMillis
    // Round so a DST shift inside the span doesn't turn 24h ± 1h into an off-by-one.
    return Math.round((end - start).toDouble() / DAY_MILLIS).toInt()
}

private fun startOfDay(timeMillis: Long): Calendar = Calendar.getInstance().apply {
    this.timeInMillis = timeMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
