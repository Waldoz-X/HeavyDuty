package com.example.heavyduty.core.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingReminderTest {

    private val zone = ZoneId.of("America/Mexico_City")

    private fun at(text: String): Long = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `before the reminder's time it fires later today, after it tomorrow`() {
        assertEquals(at("2026-10-10T18:00"), nextReminderAt(at("2026-10-10T09:30"), hour = 18, minute = 0, zone = zone))
        assertEquals(at("2026-10-11T18:00"), nextReminderAt(at("2026-10-10T18:30"), hour = 18, minute = 0, zone = zone))
        // Exactly at the time it has just fired: the next one is tomorrow's.
        assertEquals(at("2026-10-11T18:00"), nextReminderAt(at("2026-10-10T18:00"), hour = 18, minute = 0, zone = zone))
    }

    // Saturday 10 October 2026.
    private val saturday = LocalDate.of(2026, 10, 10)
    private val legs = Routine(id = 1, name = "Pierna")
    private val upper = Routine(id = 2, name = "Torso")
    private val schedule = mapOf(
        6 to listOf(CalendarEntry(id = 1, routineId = 1, dayOfWeek = 6), CalendarEntry(id = 2, routineId = 2, dayOfWeek = 6)),
        1 to listOf(CalendarEntry(id = 3, routineId = 2, dayOfWeek = 1))
    )

    private fun completed(routineId: Long, at: String) =
        WorkoutSession(id = routineId * 10, routineId = routineId, startTime = at(at), status = WorkoutStatus.COMPLETED)

    @Test
    fun `the day's routines are due until they're trained that day`() {
        val due = pendingRoutinesOn(saturday, schedule, listOf(legs, upper), sessions = emptyList(), zone = zone)
        assertEquals(listOf("Pierna", "Torso"), due.map { it.name })

        val afterLegs = pendingRoutinesOn(saturday, schedule, listOf(legs, upper), listOf(completed(1, "2026-10-10T08:00")), zone)
        assertEquals(listOf("Torso"), afterLegs.map { it.name })
    }

    @Test
    fun `training it on another day, or not finishing it, doesn't count`() {
        val sessions = listOf(
            completed(1, "2026-10-09T19:00"), // Friday
            WorkoutSession(id = 99, routineId = 2, startTime = at("2026-10-10T08:00"), status = WorkoutStatus.IN_PROGRESS)
        )

        assertEquals(2, pendingRoutinesOn(saturday, schedule, listOf(legs, upper), sessions, zone).size)
    }

    @Test
    fun `a rest day, a paused entry or a deleted routine reminds of nothing`() {
        val sunday = saturday.plusDays(1)
        assertTrue(pendingRoutinesOn(sunday, schedule, listOf(legs, upper), emptyList(), zone).isEmpty())

        val paused = mapOf(6 to listOf(CalendarEntry(routineId = 1, dayOfWeek = 6, isActive = false)))
        assertTrue(pendingRoutinesOn(saturday, paused, listOf(legs), emptyList(), zone).isEmpty())

        assertEquals(listOf("Torso"), pendingRoutinesOn(saturday, schedule, listOf(upper), emptyList(), zone).map { it.name })
    }
}
