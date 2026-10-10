package com.example.heavyduty.feature.calendar.domain.usecase

import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.repository.CalendarRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve the weekly schedule as a map of day → calendar entries.
 */
class GetWeekScheduleUseCase @Inject constructor(private val calendarRepository: CalendarRepository) {
    operator fun invoke(): Flow<Map<Int, List<CalendarEntry>>> = calendarRepository.getWeekSchedule()
}
