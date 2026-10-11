package com.example.heavyduty.core.data.health

import android.util.Log
import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.HealthSyncResult
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.HealthSync
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

@Singleton
internal class HealthSyncImpl @Inject constructor(
    private val source: HealthConnectSource,
    private val settings: HealthConnectSettings,
    private val workoutRepository: WorkoutRepository,
    private val bodyWeightRepository: BodyWeightRepository,
    private val clock: Clock
) : HealthSync {

    override val permissions: Set<String> = HealthPermissions.ALL

    override val enabled: Flow<Boolean> = settings.enabled

    override fun status(): HealthConnectStatus = source.status()

    override suspend fun grantedPermissions(): Set<String> =
        if (status() == HealthConnectStatus.AVAILABLE) quietly { source.grantedPermissions() }.orEmpty() else emptySet()

    override suspend fun connect(): HealthSyncResult {
        settings.setEnabled(true)
        return syncNow()
    }

    override suspend fun disconnect() = settings.setEnabled(false)

    override suspend fun syncNow(): HealthSyncResult {
        val granted = activePermissions()
        return HealthSyncResult(
            workoutsExported = if (HealthPermissions.WRITE_EXERCISE in granted) exportAllWorkouts() else 0,
            weightsImported = if (HealthPermissions.READ_WEIGHT in granted) importWeights() else 0
        )
    }

    override suspend fun importNewWeights() {
        quietly { if (HealthPermissions.READ_WEIGHT in activePermissions()) importWeights() }
    }

    override suspend fun workoutSaved(sessionId: Long) {
        quietly {
            if (HealthPermissions.WRITE_EXERCISE !in activePermissions()) return@quietly
            val session = workoutRepository.getSessionById(sessionId).firstOrNull()
            if (session != null && session.isExportable()) source.upsertWorkouts(listOf(session))
        }
    }

    override suspend fun workoutDeleted(sessionId: Long) {
        quietly { if (HealthPermissions.WRITE_EXERCISE in activePermissions()) source.deleteWorkout(sessionId) }
    }

    /** What may be synced right now: nothing while the link is off or Health Connect is missing. */
    private suspend fun activePermissions(): Set<String> =
        if (settings.enabled.first() && status() == HealthConnectStatus.AVAILABLE) source.grantedPermissions() else emptySet()

    private suspend fun exportAllWorkouts(): Int {
        val workouts = workoutRepository.getAllSessions().first().filter { it.isExportable() }
        source.upsertWorkouts(workouts)
        return workouts.size
    }

    private suspend fun importWeights(): Int {
        val now = clock.nowMillis()
        // A day of overlap: a scale's app may hand its reading to Health Connect late.
        val from = settings.weightsImportedUntil.first()?.let { it - IMPORT_OVERLAP_MILLIS } ?: (now - FIRST_IMPORT_MILLIS)
        val incoming = source.readWeights(from, now)
        val existing = bodyWeightRepository.getEntriesByDateRange(from - SAME_WEIGH_IN_MILLIS, now + SAME_WEIGH_IN_MILLIS).first()
        val fresh = newWeighIns(incoming, existing)
        fresh.forEach { bodyWeightRepository.logEntry(it.weightKg, it.loggedAt) }
        settings.setWeightsImportedUntil(now)
        return fresh.size
    }
}

/** Health Connect is optional: a failure there is logged and the app goes on. */
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> quietly(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w("HealthSync", "Health Connect sync failed", e)
    null
}

/** Only finished workouts that lasted something: Health Connect rejects a session ending where it starts. */
internal fun WorkoutSession.isExportable(): Boolean = status == WorkoutStatus.COMPLETED && endMillis() > startTime

/**
 * The weigh-ins in [incoming] that aren't already logged: the same weight (to 50 g) within a
 * minute of an existing entry — or of another incoming one — is the same weigh-in, so syncing
 * twice, or a reading that's also been typed in by hand, isn't counted twice.
 */
internal fun newWeighIns(incoming: List<BodyWeightEntry>, existing: List<BodyWeightEntry>): List<BodyWeightEntry> {
    val kept = existing.toMutableList()
    val fresh = mutableListOf<BodyWeightEntry>()
    incoming.sortedBy { it.loggedAt }.forEach { weighIn ->
        val duplicate = kept.any {
            abs(it.loggedAt - weighIn.loggedAt) < SAME_WEIGH_IN_MILLIS &&
                abs(it.weightKg - weighIn.weightKg) < SAME_WEIGHT_KG
        }
        if (!duplicate && weighIn.weightKg > 0f) {
            fresh += weighIn
            kept += weighIn
        }
    }
    return fresh
}

private val IMPORT_OVERLAP_MILLIS = TimeUnit.DAYS.toMillis(1)

// Without the history permission Health Connect only hands over the last 30 days anyway.
private val FIRST_IMPORT_MILLIS = TimeUnit.DAYS.toMillis(30)
private val SAME_WEIGH_IN_MILLIS = TimeUnit.MINUTES.toMillis(1)
private const val SAME_WEIGHT_KG = 0.05f
