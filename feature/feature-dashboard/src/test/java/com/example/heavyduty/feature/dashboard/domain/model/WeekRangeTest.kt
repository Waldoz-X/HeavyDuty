package com.example.heavyduty.feature.dashboard.domain.model

import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekRangeTest {

    private val zone = ZoneId.systemDefault()

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `week runs from Monday midnight to the next Monday, whatever the locale`() {
        // Wednesday 2026-09-30.
        val range = weekRangeContaining(at(2026, 9, 30, 10, 0))

        assertEquals(at(2026, 9, 28, 0, 0), range.first)
        assertEquals(at(2026, 10, 5, 0, 0), range.last + 1)
    }

    @Test
    fun `Sunday belongs to the week that started the Monday before`() {
        val sunday = at(2026, 10, 4, 23, 59)

        val range = weekRangeContaining(sunday)

        assertEquals(at(2026, 9, 28, 0, 0), range.first)
        assertTrue(sunday in range)
        assertFalse(at(2026, 10, 5, 0, 0) in range)
    }

    @Test
    fun `range still covers workouts logged later the same week`() {
        val openedAt = at(2026, 9, 30, 10, 0)
        val loggedLater = at(2026, 9, 30, 10, 30)

        assertTrue(loggedLater in weekRangeContaining(openedAt))
    }

    @Test
    fun `current week flow starts with the week containing now`() = runBlocking {
        val now = at(2026, 9, 30)

        assertEquals(weekRangeContaining(now), currentWeekRanges { now }.first())
    }

    @Test
    fun `calendar days ignore the time of day`() {
        val lastNight = at(2026, 9, 29, 22, 0)
        val thisMorning = at(2026, 9, 30, 9, 0)
        val earlierToday = at(2026, 9, 30, 7, 0)

        assertEquals(1, calendarDaysBetween(lastNight, thisMorning))
        assertEquals(0, calendarDaysBetween(earlierToday, thisMorning))
        assertEquals(3, calendarDaysBetween(at(2026, 9, 27), thisMorning))
    }
}
