package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.HealthSyncResult
import kotlinx.coroutines.flow.Flow

/**
 * Keeps Health Connect in step with a workout that was finished, edited or deleted — when the
 * user connected it; otherwise, and whenever Health Connect fails, it does nothing: the app's
 * own flow never waits on it or breaks because of it.
 */
interface WorkoutHealthMirror {
    suspend fun workoutSaved(sessionId: Long)
    suspend fun workoutDeleted(sessionId: Long)

    companion object {
        /** For code paths (and tests) where Health Connect doesn't take part. */
        val NONE: WorkoutHealthMirror = object : WorkoutHealthMirror {
            override suspend fun workoutSaved(sessionId: Long) = Unit
            override suspend fun workoutDeleted(sessionId: Long) = Unit
        }
    }
}

/**
 * The Health Connect link, as Perfil shows it: finished workouts go out as exercise sessions,
 * weigh-ins (from scales and other apps) come in as body-weight entries.
 */
interface HealthSync : WorkoutHealthMirror {
    /** The Health Connect permissions the app asks for (granted on Health Connect's own screen). */
    val permissions: Set<String>

    /** The user's choice: on after connecting, off after disconnecting. */
    val enabled: Flow<Boolean>

    fun status(): HealthConnectStatus

    /** The [permissions] currently granted; empty when Health Connect can't be used. */
    suspend fun grantedPermissions(): Set<String>

    /** Turns syncing on and sends/brings everything once. Throws if Health Connect fails. */
    suspend fun connect(): HealthSyncResult

    /** Stops syncing. What was already sent stays in Health Connect. */
    suspend fun disconnect()

    /** Sends every finished workout and brings new weigh-ins. Throws if Health Connect fails. */
    suspend fun syncNow(): HealthSyncResult

    /** Brings weigh-ins logged since the last time, quietly (on opening the app). */
    suspend fun importNewWeights()
}
