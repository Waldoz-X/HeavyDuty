package com.example.heavyduty.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TrainingStreakTest {

    private fun at(day: Int, hour: Int = 18, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    private val now = at(8, hour = 12) // Thursday Oct 8, midday

    private fun workout(day: Int, hour: Int = 18, minute: Int = 0, status: WorkoutStatus = WorkoutStatus.COMPLETED) =
        WorkoutSession(startTime = at(day, hour, minute), status = status)

    private fun streak(vararg sessions: WorkoutSession) = currentStreakDays(sessions.toList(), now)

    @Test
    fun `consecutive days up to today`() {
        assertEquals(3, streak(workout(8, hour = 7), workout(7), workout(6)))
    }

    @Test
    fun `not having trained yet today keeps yesterday's streak`() {
        assertEquals(2, streak(workout(7), workout(6)))
    }

    @Test
    fun `a missed day ends the streak`() {
        assertEquals(1, streak(workout(8, hour = 7), workout(6), workout(5)))
        assertEquals(0, streak(workout(6), workout(5)))
    }

    @Test
    fun `two workouts on the same day count once`() {
        assertEquals(2, streak(workout(8, hour = 7), workout(8, hour = 11), workout(7)))
    }

    @Test
    fun `a late-night workout counts for the day it started`() {
        assertEquals(2, streak(workout(7, hour = 23, minute = 50), workout(6)))
    }

    @Test
    fun `unfinished or cancelled workouts don't count`() {
        assertEquals(0, streak(workout(8, hour = 7, status = WorkoutStatus.IN_PROGRESS)))
        assertEquals(1, streak(workout(8, hour = 7), workout(7, status = WorkoutStatus.CANCELLED)))
    }

    @Test
    fun `no workouts, no streak`() {
        assertEquals(0, streak())
    }
}
