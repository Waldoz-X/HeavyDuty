package com.example.heavyduty.core.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The daily "today's workout" reminder: off until the user turns it on, at 18:00 by default. */
data class ReminderSettings(val enabled: Boolean = false, val hour: Int = DEFAULT_REMINDER_HOUR, val minute: Int = 0)

const val DEFAULT_REMINDER_HOUR = 18

/** The next time the reminder fires after [now]: today at [hour]:[minute] if still ahead, else tomorrow. */
fun nextReminderAt(now: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val nowDateTime = Instant.ofEpochMilli(now).atZone(zone)
    val today = nowDateTime.toLocalDate().atTime(hour, minute).atZone(zone)
    val next = if (today.isAfter(nowDateTime)) today else today.plusDays(1)
    return next.toInstant().toEpochMilli()
}

/**
 * The routines the weekly calendar plans for [date] that haven't been trained that day yet — the
 * reminder's subject ("Hoy toca: Pierna"). Empty on a rest day or once everything's done, so no
 * reminder is sent then. [schedule] is keyed Monday = 1 … Sunday = 7, like the calendar.
 */
fun pendingRoutinesOn(
    date: LocalDate,
    schedule: Map<Int, List<CalendarEntry>>,
    routines: List<Routine>,
    sessions: List<WorkoutSession>,
    zone: ZoneId = ZoneId.systemDefault()
): List<Routine> {
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val trainedToday = sessions
        .filter { it.status == WorkoutStatus.COMPLETED && it.startTime in dayStart until dayEnd }
        .mapNotNull { it.routineId }
        .toSet()
    val routineById = routines.associateBy { it.id }
    return schedule[date.dayOfWeek.value].orEmpty()
        .filter { it.isActive }
        .mapNotNull { routineById[it.routineId] }
        .filter { it.id !in trainedToday }
        .distinctBy { it.id }
}
