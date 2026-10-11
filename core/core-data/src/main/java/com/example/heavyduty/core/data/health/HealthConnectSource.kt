package com.example.heavyduty.core.data.health

import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.WorkoutSession

/** The few Health Connect operations the sync needs — a seam so its rules can be tested without it. */
internal interface HealthConnectSource {
    fun status(): HealthConnectStatus
    suspend fun grantedPermissions(): Set<String>

    /** Writes these workouts, replacing the ones already sent (matched by the workout's id). */
    suspend fun upsertWorkouts(sessions: List<WorkoutSession>)
    suspend fun deleteWorkout(sessionId: Long)

    /** Weigh-ins from any app between [from] and [until] (epoch millis), as body-weight entries. */
    suspend fun readWeights(from: Long, until: Long): List<BodyWeightEntry>
}

/** Must match the <uses-permission> entries in the app's manifest. */
internal object HealthPermissions {
    const val WRITE_EXERCISE = "android.permission.health.WRITE_EXERCISE"
    const val READ_WEIGHT = "android.permission.health.READ_WEIGHT"
    val ALL = setOf(WRITE_EXERCISE, READ_WEIGHT)
}

/** Where a finished workout ends; older rows may lack endTime but have their duration. */
internal fun WorkoutSession.endMillis(): Long = endTime ?: (startTime + totalDurationSecs * MILLIS_PER_SECOND)

private const val MILLIS_PER_SECOND = 1_000L
