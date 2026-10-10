package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingTotalsTest {

    @Test
    fun `no sessions gives zero totals`() {
        assertEquals(TrainingTotals(), trainingTotals(emptyList(), emptyList()))
    }

    @Test
    fun `only completed sessions and their working sets count`() {
        val sessions = listOf(
            WorkoutSession(id = 1, totalDurationSecs = 3600, status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 2, totalDurationSecs = 900, status = WorkoutStatus.IN_PROGRESS)
        )
        val sets = listOf(
            WorkoutSet(sessionId = 1, reps = 10, weight = 100f, isCompleted = true),
            WorkoutSet(sessionId = 1, reps = 5, weight = 60f, isCompleted = true, isWarmup = true),
            WorkoutSet(sessionId = 1, reps = null, durationSecs = 60, isCompleted = true),
            // Logged in a workout that is still running (or was abandoned).
            WorkoutSet(sessionId = 2, reps = 8, weight = 80f, isCompleted = true)
        )

        val totals = trainingTotals(sessions, sets)

        assertEquals(1, totals.workouts)
        assertEquals(3600, totals.durationSecs)
        assertEquals(1000f, totals.volumeKg, 0.001f)
        assertEquals(2, totals.sets)
    }
}
