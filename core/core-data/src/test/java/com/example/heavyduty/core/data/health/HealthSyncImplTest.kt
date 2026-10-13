package com.example.heavyduty.core.data.health

import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.HealthSyncResult
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeBodyWeightRepository
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeWorkoutRepository
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthSyncImplTest {

    private val hour = 3_600_000L
    private val now = 1_800_000_000_000L

    private val finished =
        WorkoutSession(
            id = 1,
            sessionName = "Torso",
            startTime = now - 30 * hour,
            endTime = now - 29 * hour,
            status = WorkoutStatus.COMPLETED
        )
    private val inProgress = WorkoutSession(id = 2, startTime = now - hour, status = WorkoutStatus.IN_PROGRESS)
    private val zeroLength = WorkoutSession(id = 3, startTime = now - 5 * hour, endTime = now - 5 * hour, status = WorkoutStatus.COMPLETED)

    private val healthConnect = FakeHealthConnect()
    private val settings = FakeSettings()
    private val workouts = FakeWorkoutRepository(listOf(finished, inProgress, zeroLength))
    private val bodyWeights = FakeBodyWeightRepository()
    private val sync = HealthSyncImpl(healthConnect, settings, workouts, workouts.history, bodyWeights, FakeClock(now))

    @Test
    fun `connecting sends the finished workouts and brings the weigh-ins`() = runTest {
        healthConnect.weights += BodyWeightEntry(weightKg = 80.4f, loggedAt = now - 2 * hour)

        val result = sync.connect()

        assertEquals(HealthSyncResult(workoutsExported = 1, weightsImported = 1), result)
        assertEquals(listOf(1L), healthConnect.workouts.keys.toList())
        assertEquals(listOf(80.4f), bodyWeights.entries.value.map { it.weightKg })
        assertTrue(settings.enabled.value)
    }

    @Test
    fun `syncing twice doesn't duplicate weigh-ins, nor ones already typed in`() = runTest {
        bodyWeights.logEntry(80.4f, now - 2 * hour + 20_000)
        healthConnect.weights += BodyWeightEntry(weightKg = 80.4f, loggedAt = now - 2 * hour)
        healthConnect.weights += BodyWeightEntry(weightKg = 80.1f, loggedAt = now - 26 * hour)

        sync.connect()
        val second = sync.syncNow()

        assertEquals(0, second.weightsImported)
        assertEquals(listOf(80.1f, 80.4f), bodyWeights.entries.value.map { it.weightKg }.sorted())
    }

    @Test
    fun `only what was granted is synced`() = runTest {
        healthConnect.granted = setOf(HealthPermissions.READ_WEIGHT)
        healthConnect.weights += BodyWeightEntry(weightKg = 80f, loggedAt = now - hour)

        val result = sync.connect()

        assertEquals(HealthSyncResult(workoutsExported = 0, weightsImported = 1), result)
        assertTrue(healthConnect.workouts.isEmpty())
    }

    @Test
    fun `nothing leaves the app while the link is off`() = runTest {
        sync.workoutSaved(1)
        sync.importNewWeights()

        assertTrue(healthConnect.workouts.isEmpty())
        assertEquals(0, healthConnect.reads)
    }

    @Test
    fun `an edited workout replaces the sent one and a deleted one leaves`() = runTest {
        sync.connect()
        workouts.updateSession(finished.copy(endTime = finished.startTime + 2 * hour))

        sync.workoutSaved(1)
        assertEquals(finished.startTime + 2 * hour, healthConnect.workouts.getValue(1).endTime)

        sync.workoutDeleted(1)
        assertTrue(healthConnect.workouts.isEmpty())
    }

    @Test
    fun `a Health Connect failure never reaches finishing or deleting a workout`() = runTest {
        sync.connect()
        healthConnect.failing = true

        sync.workoutSaved(1)
        sync.workoutDeleted(1)
        sync.importNewWeights()
        // Reaching here without an exception is the point.
    }

    @Test
    fun `weigh-ins are read from a day before the last import`() = runTest {
        settings.weightsImportedUntil.value = now - 10 * hour
        sync.disconnect()
        settings.enabled.value = true

        sync.importNewWeights()

        assertEquals(now - 34 * hour, healthConnect.lastReadFrom)
        assertEquals(now, settings.weightsImportedUntil.value)
    }

    @Test
    fun `a weigh-in near another one but different weight is new`() {
        val existing = listOf(BodyWeightEntry(weightKg = 80f, loggedAt = now))

        val fresh = newWeighIns(listOf(BodyWeightEntry(weightKg = 81f, loggedAt = now + 10_000)), existing)

        assertEquals(1, fresh.size)
    }
}

private class FakeHealthConnect : HealthConnectSource {
    var granted: Set<String> = HealthPermissions.ALL
    var failing = false
    val workouts = mutableMapOf<Long, WorkoutSession>()
    val weights = mutableListOf<BodyWeightEntry>()
    var reads = 0
    var lastReadFrom: Long? = null

    override fun status() = HealthConnectStatus.AVAILABLE
    override suspend fun grantedPermissions(): Set<String> = granted

    override suspend fun upsertWorkouts(sessions: List<WorkoutSession>) {
        if (failing) throw IOException("Health Connect is down")
        sessions.forEach { workouts[it.id] = it }
    }

    override suspend fun deleteWorkout(sessionId: Long) {
        if (failing) throw IOException("Health Connect is down")
        workouts.remove(sessionId)
    }

    override suspend fun readWeights(from: Long, until: Long): List<BodyWeightEntry> {
        if (failing) throw IOException("Health Connect is down")
        reads++
        lastReadFrom = from
        return weights.filter { it.loggedAt in from..until }
    }
}

private class FakeSettings : HealthConnectSettings {
    override val enabled = MutableStateFlow(false)
    override suspend fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    override val weightsImportedUntil = MutableStateFlow<Long?>(null)
    override suspend fun setWeightsImportedUntil(millis: Long) {
        weightsImportedUntil.value = millis
    }
}
