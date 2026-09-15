package com.example.heavyduty.core.ui.components

/**
 * A rolling date-range option for stats screens ("last 7 days", "last 30 days", …), shared so
 * every screen with a period dropdown ([HdPeriodSelector]) computes the same windows the same
 * way — including the "previous period" used for trend deltas.
 */
enum class StatsPeriod(val days: Int, val label: String) {
    LAST_7_DAYS(7, "Últimos 7 días"),
    LAST_30_DAYS(30, "Últimos 30 días"),
    LAST_90_DAYS(90, "Últimos 90 días"),
}

/** [now - days, now] and the immediately preceding, equal-length window — for trend deltas. */
fun StatsPeriod.currentAndPreviousRange(now: Long = System.currentTimeMillis()): Pair<LongRange, LongRange> {
    val spanMillis = days * 24L * 60L * 60L * 1000L
    val currentStart = now - spanMillis
    val previousEnd = currentStart - 1
    val previousStart = previousEnd - spanMillis
    return (currentStart..now) to (previousStart..previousEnd)
}
