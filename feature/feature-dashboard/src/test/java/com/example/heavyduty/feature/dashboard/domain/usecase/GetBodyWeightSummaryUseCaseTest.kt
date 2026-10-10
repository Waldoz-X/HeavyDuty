package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.testing.FakeBodyWeightRepository
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBodyWeightSummaryUseCaseTest {

    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `a weight logged after the card started watching shows up right away`() = runBlocking {
        val clock = FakeClock(100 * day)
        val weights = FakeBodyWeightRepository()
        val summary = GetBodyWeightSummaryUseCase(weights, FakeUserPreferencesRepository(), clock)()
        weights.logEntry(82f, loggedAt = 99 * day)
        assertEquals(82f, summary.first().currentKg)

        // The user logs today's weight a minute later, from the dashboard's own sheet.
        clock.advanceBy(60_000)
        weights.logEntry(80.5f, loggedAt = clock.now)

        val updated = summary.first()
        assertEquals(80.5f, updated.currentKg)
        assertEquals(-1.5f, updated.deltaKg!!, 0.001f)
    }

    @Test
    fun `only the last 90 days are charted`() = runBlocking {
        val clock = FakeClock(200 * day)
        val weights = FakeBodyWeightRepository()
        weights.logEntry(90f, loggedAt = 100 * day) // 100 days ago
        weights.logEntry(85f, loggedAt = 150 * day)

        val summary = GetBodyWeightSummaryUseCase(weights, FakeUserPreferencesRepository(), clock)().first()

        assertEquals(listOf(150 * day to 85f), summary.chartPoints)
    }
}
