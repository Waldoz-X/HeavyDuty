package com.example.heavyduty.core.domain.model

/**
 * Domain model representing a single body-weight log entry.
 */
data class BodyWeightEntry(
    val id: Long = 0,
    val weightKg: Float,
    val loggedAt: Long = System.currentTimeMillis(),
)
