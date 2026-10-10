package com.example.heavyduty.feature.history.domain.model

/**
 * Personal records for a single exercise, derived from its logged set history.
 */
data class ExercisePersonalRecords(
    val maxWeight: Float? = null,
    val maxWeightReps: Int? = null,
    val maxWeightAchievedAtMillis: Long? = null,
    val maxReps: Int? = null,
    val maxRepsWeight: Float? = null,
    val estimatedOneRepMax: Float? = null
) {
    val hasRecords: Boolean get() = maxWeight != null
}
