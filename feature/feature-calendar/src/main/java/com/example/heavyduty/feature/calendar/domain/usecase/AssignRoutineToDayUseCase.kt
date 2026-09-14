package com.example.heavyduty.feature.calendar.domain.usecase

import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Recurrence
import com.example.heavyduty.core.domain.repository.CalendarRepository
import javax.inject.Inject

/**
 * Use case to assign a routine to a specific day of the week.
 */
class AssignRoutineToDayUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
) {
    suspend operator fun invoke(routineId: Long, dayOfWeek: Int): Long {
        if (calendarRepository.hasRoutineOnDay(routineId, dayOfWeek)) {
            throw IllegalStateException("DuplicateRoutine")
        }
        val entry = CalendarEntry(
            routineId = routineId,
            dayOfWeek = dayOfWeek,
            recurrence = Recurrence.WEEKLY,
            isActive = true,
        )
        return calendarRepository.insertEntry(entry)
    }
}
