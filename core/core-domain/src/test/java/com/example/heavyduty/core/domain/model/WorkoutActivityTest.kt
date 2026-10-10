package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutActivityTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val start = 1_800_000_000_000

    private fun setAt(at: Long?) = WorkoutSet(isCompleted = true, completedAt = at)

    @Test
    fun `the last activity is the latest set, or the start when nothing was logged`() {
        assertEquals(start, lastActivityAt(start, emptyList()))
        assertEquals(start + 40 * minute, lastActivityAt(start, listOf(setAt(start + 10 * minute), setAt(start + 40 * minute))))
        // A set without a timestamp can't move it.
        assertEquals(start, lastActivityAt(start, listOf(setAt(null))))
    }

    @Test
    fun `finished right after training, a workout ends when it's closed`() {
        val sets = listOf(setAt(start + 55 * minute))

        // Closed after a normal rest: the cool-down counts.
        assertEquals(start + 60 * minute, workoutEndTime(start, sets, now = start + 60 * minute))
        // Right on the limit still counts as part of the workout.
        assertEquals(start + 85 * minute, workoutEndTime(start, sets, now = start + 85 * minute))
    }

    @Test
    fun `left open long after the last set, a workout ends at that set`() {
        // What the app recorded as 35 hours: an hour of training, then left running overnight.
        val sets = listOf(setAt(start + 20 * minute), setAt(start + hour))

        assertEquals(start + hour, workoutEndTime(start, sets, now = start + 35 * hour))
    }

    @Test
    fun `a workout opened and forgotten without a single set ends where it began`() {
        assertEquals(start, workoutEndTime(start, emptyList(), now = start + 5 * hour))
    }

    @Test
    fun `three hours without logging anything is a forgotten workout`() {
        assertFalse(isForgotten(lastActivityAt = start, now = start + 2 * hour + 59 * minute))
        assertTrue(isForgotten(lastActivityAt = start, now = start + 3 * hour))
    }
}
