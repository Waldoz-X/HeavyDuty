package com.example.heavyduty.core.data.health

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class HealthConnectClientSource @Inject constructor(@param:ApplicationContext private val context: Context) :
    HealthConnectSource {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override fun status(): HealthConnectStatus = when {
        // The client library itself needs Android 8; the app still runs on 7.
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> HealthConnectStatus.UNSUPPORTED

        else -> when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.NEEDS_INSTALL
            else -> HealthConnectStatus.UNSUPPORTED
        }
    }

    override suspend fun grantedPermissions(): Set<String> = client.permissionController.getGrantedPermissions()

    override suspend fun upsertWorkouts(sessions: List<WorkoutSession>) {
        // A record with a clientRecordId that's already there replaces it, so sending a
        // workout again (after editing its duration, or on "sync now") never duplicates it.
        val version = System.currentTimeMillis()
        sessions.chunked(WRITE_BATCH).forEach { batch -> client.insertRecords(batch.map { it.toRecord(version) }) }
    }

    override suspend fun deleteWorkout(sessionId: Long) {
        client.deleteRecords(ExerciseSessionRecord::class, recordIdsList = emptyList(), clientRecordIdsList = listOf(recordId(sessionId)))
    }

    override suspend fun readWeights(from: Long, until: Long): List<BodyWeightEntry> {
        val weights = mutableListOf<BodyWeightEntry>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(Instant.ofEpochMilli(from), Instant.ofEpochMilli(until)),
                    pageToken = pageToken
                )
            )
            response.records.mapTo(weights) {
                BodyWeightEntry(weightKg = it.weight.inKilograms.toFloat(), loggedAt = it.time.toEpochMilli())
            }
            pageToken = response.pageToken
        } while (pageToken != null)
        return weights
    }
}

private fun WorkoutSession.toRecord(version: Long): ExerciseSessionRecord {
    val start = Instant.ofEpochMilli(startTime)
    val end = Instant.ofEpochMilli(endMillis())
    val zone = ZoneId.systemDefault().rules
    return ExerciseSessionRecord(
        startTime = start,
        startZoneOffset = zone.getOffset(start),
        endTime = end,
        endZoneOffset = zone.getOffset(end),
        // Logged set by set by the user rather than measured by a sensor.
        metadata = Metadata.manualEntry(clientRecordId = recordId(id), clientRecordVersion = version),
        exerciseType = type.healthExerciseType(),
        title = sessionName.ifBlank { null },
        notes = notes.ifBlank { null }
    )
}

private fun WorkoutType.healthExerciseType(): Int = when (this) {
    WorkoutType.GYM, WorkoutType.MIXED -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    WorkoutType.CROSSFIT -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
    WorkoutType.CARDIO -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
}

/** Stable per workout, so Health Connect knows a re-sent workout is the same one. */
private fun recordId(sessionId: Long) = "heavyduty-workout-$sessionId"

private const val WRITE_BATCH = 100
