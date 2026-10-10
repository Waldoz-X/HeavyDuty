package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneRepMaxTest {

    @Test
    fun `best estimate is the highest working set`() {
        val sets = listOf(
            WorkoutSet(reps = 10, weight = 60f), // 80
            WorkoutSet(reps = 5, weight = 75f), // 87.5
            WorkoutSet(reps = 1, weight = 85f) // 85, measured
        )
        assertEquals(87.5f, sets.bestEstimatedOneRepMax()!!, 0.001f)
    }

    @Test
    fun `warm-ups and sets without weight or reps are ignored`() {
        val sets = listOf(
            WorkoutSet(reps = 3, weight = 100f, isWarmup = true),
            WorkoutSet(reps = null, weight = 120f, durationSecs = 30),
            WorkoutSet(reps = 8, weight = null),
            WorkoutSet(reps = 6, weight = 60f) // 72
        )
        assertEquals(72f, sets.bestEstimatedOneRepMax()!!, 0.001f)
    }

    @Test
    fun `nothing to estimate gives null`() {
        assertNull(emptyList<WorkoutSet>().bestEstimatedOneRepMax())
        assertNull(listOf(WorkoutSet(reps = 20, weight = 40f)).bestEstimatedOneRepMax())
    }
}
