package com.example.heavyduty.core.ui.components

/**
 * A single entry in the workout activity feed, shown on the profile screen.
 */
data class WorkoutFeedItem(
    val id: Long,
    val userName: String,
    val userAvatarUrl: String?,
    val relativeTime: String,
    val workoutName: String,
    val durationMinutes: Int,
    val volumeKg: Float,
    val personalRecords: Int,
    val exercises: List<String>
)
