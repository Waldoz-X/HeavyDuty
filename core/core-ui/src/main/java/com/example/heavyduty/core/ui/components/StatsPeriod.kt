package com.example.heavyduty.core.ui.components

import androidx.annotation.StringRes
import com.example.heavyduty.core.common.R

/**
 * A rolling date-range option for stats screens ("last 7 days", "last 30 days", …), shared so
 * every screen with a period dropdown ([HdPeriodSelector]) computes the same windows the same
 * way — including the "previous period" used for trend deltas.
 */
enum class StatsPeriod(val days: Int, @param:StringRes val labelRes: Int) {
    LAST_7_DAYS(7, R.string.period_last_7_days),
    LAST_30_DAYS(30, R.string.period_last_30_days)
}

/**
 * The last [StatsPeriod.days] calendar days including today (through the end of today, so
 * sets logged after the screen opened still count) and the immediately preceding,
 * equal-length window — for trend deltas.
 */
fun StatsPeriod.currentAndPreviousRange(now: Long = System.currentTimeMillis()): Pair<LongRange, LongRange> {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    val currentEnd = calendar.timeInMillis - 1
    calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
    val currentStart = calendar.timeInMillis
    calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
    val previousStart = calendar.timeInMillis
    return (currentStart..currentEnd) to (previousStart until currentStart)
}
