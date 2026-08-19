package com.example.heavyduty.feature.profile.domain.model

/**
 * Aggregated training stats derived from workout history, shown on the profile screen.
 */
data class ProfileStats(
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalVolumeKg: Float = 0f,
    val memberSinceMillis: Long? = null,
)
