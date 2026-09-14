package com.example.heavyduty.feature.stats.domain.model

import com.example.heavyduty.core.domain.model.MuscleGroup

/**
 * Aggregated training summary for a single calendar month.
 */
data class MonthlyReport(
    val monthLabel: String = "",
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalVolumeKg: Float = 0f,
    val avgSessionDurationMinutes: Int = 0,
    val topMuscleGroup: MuscleGroup? = null,
    val workoutsPerWeek: List<Pair<String, Int>> = emptyList(),
    val volumePerWeek: List<Pair<String, Float>> = emptyList(),
)
