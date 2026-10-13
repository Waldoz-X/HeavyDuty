package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.testing.FakeCalendarRepository
import com.example.heavyduty.core.testing.FakeClock
import com.example.heavyduty.core.testing.FakeRoutineRepository
import com.example.heavyduty.core.testing.FakeTrainingHistoryRepository
import com.example.heavyduty.feature.dashboard.domain.model.DayStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The home screen's week strip, driven by the calendar: what was planned, done or missed. */
class GetWeeklyScheduleOverviewUseCaseTest {

    private val thursday = LocalDate.of(2026, 10, 8)
    private fun millis(date: LocalDate, hour: Int = 18) =
        date.atTime(LocalTime.of(hour, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val torso = Routine(id = 10, name = "Torso")
    private val legs = Routine(id = 11, name = "Pierna")
    private val schedule = FakeCalendarRepository(
        listOf(
            CalendarEntry(id = 1, routineId = 10, dayOfWeek = 1), // Mon: Torso — done
            CalendarEntry(id = 2, routineId = 11, dayOfWeek = 2), // Tue: Legs — missed
            CalendarEntry(id = 3, routineId = 10, dayOfWeek = 4), // Thu (today): Torso AM…
            CalendarEntry(id = 4, routineId = 11, dayOfWeek = 4), // …and Legs PM, only one done
            CalendarEntry(id = 5, routineId = 10, dayOfWeek = 6), // Sat: upcoming
            CalendarEntry(id = 6, routineId = 99, dayOfWeek = 7) // Sun: a routine that was deleted
        )
    )
    private val workouts = FakeTrainingHistoryRepository(
        listOf(
            WorkoutSession(id = 1, routineId = 10, startTime = millis(thursday.minusDays(3)), status = WorkoutStatus.COMPLETED),
            WorkoutSession(id = 2, routineId = 10, startTime = millis(thursday, hour = 7), status = WorkoutStatus.COMPLETED),
            // Wednesday was a rest day, but they trained anyway.
            WorkoutSession(id = 3, routineId = null, startTime = millis(thursday.minusDays(1)), status = WorkoutStatus.COMPLETED),
            // Started Tuesday's routine but never finished it: still missed.
            WorkoutSession(id = 4, routineId = 11, startTime = millis(thursday.minusDays(2)), status = WorkoutStatus.IN_PROGRESS)
        )
    )

    private fun week(offset: Int = 0) = runBlocking {
        GetWeeklyScheduleOverviewUseCase(
            schedule,
            FakeRoutineRepository(listOf(torso, legs)),
            workouts,
            FakeClock(millis(thursday, hour = 12))
        )(offset).first()
    }

    @Test
    fun `each day of this week gets the right status`() {
        val days = week()

        assertEquals((5..11).toList(), days.map { it.dayNumber }) // Mon Oct 5 … Sun Oct 11
        assertEquals(
            listOf(
                DayStatus.COMPLETED,
                DayStatus.MISSED,
                DayStatus.COMPLETED,
                DayStatus.TODAY_PENDING,
                DayStatus.REST,
                DayStatus.UPCOMING,
                DayStatus.REST
            ),
            days.map { it.status }
        )
        assertEquals(listOf(false, false, false, true, false, false, false), days.map { it.isToday })
    }

    @Test
    fun `a day with two routines shows both and which one is done`() {
        val today = week()[3]

        assertEquals(listOf("Torso" to true, "Pierna" to false), today.routines.map { it.routineName to it.isCompleted })
    }

    @Test
    fun `a deleted routine disappears from the plan`() {
        assertTrue(week()[6].routines.isEmpty())
    }

    @Test
    fun `next week is all upcoming, last week's unfinished days are missed`() {
        assertEquals((12..18).toList(), week(1).map { it.dayNumber })
        assertTrue(week(1).filter { it.routines.isNotEmpty() }.all { it.status == DayStatus.UPCOMING })
        assertTrue(week(-1).filter { it.routines.isNotEmpty() }.all { it.status == DayStatus.MISSED })
    }
}
