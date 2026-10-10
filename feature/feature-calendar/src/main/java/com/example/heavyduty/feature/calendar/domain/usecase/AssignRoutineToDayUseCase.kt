package com.example.heavyduty.feature.calendar.domain.usecase

import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Recurrence
import com.example.heavyduty.core.domain.repository.CalendarRepository
import javax.inject.Inject

/** The routine is already on that day; assigning it again would list it twice. */
class DuplicateCalendarEntryException : IllegalStateException("Routine already scheduled on that day")

/**
 * Use case to assign a routine to a specific day of the week.
 *
 * @throws DuplicateCalendarEntryException if the routine is already on that day.
 */
class AssignRoutineToDayUseCase @Inject constructor(private val calendarRepository: CalendarRepository) {
    suspend operator fun invoke(routineId: Long, dayOfWeek: Int): Long {
        if (calendarRepository.hasRoutineOnDay(routineId, dayOfWeek)) {
            throw DuplicateCalendarEntryException()
        }
        val entry = CalendarEntry(
            routineId = routineId,
            dayOfWeek = dayOfWeek,
            recurrence = Recurrence.WEEKLY,
            isActive = true
        )
        return calendarRepository.insertEntry(entry)
    }
}
