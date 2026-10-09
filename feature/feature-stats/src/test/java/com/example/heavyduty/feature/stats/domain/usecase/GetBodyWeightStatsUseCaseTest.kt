package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.testing.FakeBodyWeightRepository
import com.example.heavyduty.core.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetBodyWeightStatsUseCaseTest {

    private fun stats(entries: List<BodyWeightEntry>, goalKg: Float?) = runBlocking {
        GetBodyWeightStatsUseCase(FakeBodyWeightRepository(entries), FakeUserPreferencesRepository(goalKg))().first()
    }

    // Logged out of order on purpose: everything must follow the dates.
    private val losing = listOf(
        BodyWeightEntry(id = 2, weightKg = 85f, loggedAt = 2 * DAY),
        BodyWeightEntry(id = 1, weightKg = 90f, loggedAt = 1 * DAY),
        BodyWeightEntry(id = 3, weightKg = 80f, loggedAt = 3 * DAY),
    )

    @Test
    fun `summary follows the dates, not the insertion order`() {
        val report = stats(losing, goalKg = null)

        assertEquals(90f, report.startWeightKg)
        assertEquals(80f, report.currentWeightKg)
        assertEquals(90f, report.maxWeightKg)
        assertEquals(80f, report.minWeightKg)
        assertEquals(85f, report.avgWeightKg!!, 0.001f)
        assertEquals(listOf(DAY to 90f, 2 * DAY to 85f, 3 * DAY to 80f), report.chartPoints)
    }

    @Test
    fun `history is newest first with the change since the entry before`() {
        val history = stats(losing, goalKg = null).history

        assertEquals(listOf(3L, 2L, 1L), history.map { it.id })
        assertEquals(listOf(-5f, -5f, null), history.map { it.deltaKg })
    }

    @Test
    fun `losing toward a goal`() {
        val report = stats(losing, goalKg = 75f)

        assertEquals(10f / 15f, report.progressPercentage!!, 0.001f) // lost 10 of 15
        assertEquals(5f, report.remainingToGoalKg!!, 0.001f)
    }

    @Test
    fun `gaining toward a goal`() {
        val gaining = listOf(BodyWeightEntry(weightKg = 60f, loggedAt = DAY), BodyWeightEntry(weightKg = 65f, loggedAt = 2 * DAY))
        val report = stats(gaining, goalKg = 70f)

        assertEquals(0.5f, report.progressPercentage!!, 0.001f)
        assertEquals(5f, report.remainingToGoalKg!!, 0.001f)
    }

    @Test
    fun `going past the goal caps progress and shows how far past`() {
        val report = stats(losing, goalKg = 82f)

        assertEquals(1f, report.progressPercentage!!, 0.001f)
        assertEquals(-2f, report.remainingToGoalKg!!, 0.001f)
    }

    @Test
    fun `moving away from the goal is zero progress`() {
        val report = stats(losing, goalKg = 95f)

        assertEquals(0f, report.progressPercentage!!, 0.001f)
        assertEquals(15f, report.remainingToGoalKg!!, 0.001f)
    }

    @Test
    fun `no entries or no goal means no progress`() {
        val empty = stats(emptyList(), goalKg = 70f)
        assertNull(empty.currentWeightKg)
        assertNull(empty.avgWeightKg)
        assertNull(empty.progressPercentage)
        assertEquals(70f, empty.goalWeightKg)

        assertNull(stats(losing, goalKg = null).progressPercentage)
    }
}
