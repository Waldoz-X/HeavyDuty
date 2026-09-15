package com.example.heavyduty.feature.stats.domain.model

/**
 * Totals for a rolling window (see [com.example.heavyduty.core.ui.components.StatsPeriod]),
 * paired with the same totals for the immediately preceding, equal-length window so screens
 * can show "vs. previous period" deltas the same way the monthly report does for its month.
 */
data class PeriodSummary(
    val workouts: Int = 0,
    val durationMinutes: Int = 0,
    val volumeKg: Float = 0f,
    val sets: Int = 0,
    val previousWorkouts: Int = 0,
    val previousDurationMinutes: Int = 0,
    val previousVolumeKg: Float = 0f,
    val previousSets: Int = 0,
) {
    val workoutsDelta: Int get() = workouts - previousWorkouts
    val durationMinutesDelta: Int get() = durationMinutes - previousDurationMinutes
    val volumeKgDelta: Float get() = volumeKg - previousVolumeKg
    val setsDelta: Int get() = sets - previousSets
}
