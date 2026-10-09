package com.example.heavyduty.core.domain.model

import java.util.Calendar

/**
 * Consecutive days, ending today, with at least one completed workout. A day without training
 * yet today doesn't break a streak that was alive yesterday — the day isn't over. Shared by the
 * dashboard and the profile so both always show the same number.
 */
fun currentStreakDays(sessions: List<WorkoutSession>, now: Long = System.currentTimeMillis()): Int {
    val trainedDays = sessions
        .filter { it.status == WorkoutStatus.COMPLETED }
        .mapTo(HashSet()) { startOfDay(it.startTime) }
    if (trainedDays.isEmpty()) return 0

    val cursor = Calendar.getInstance().apply { timeInMillis = startOfDay(now) }
    if (cursor.timeInMillis !in trainedDays) cursor.add(Calendar.DAY_OF_YEAR, -1)

    var streak = 0
    while (startOfDay(cursor.timeInMillis) in trainedDays) {
        streak++
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
