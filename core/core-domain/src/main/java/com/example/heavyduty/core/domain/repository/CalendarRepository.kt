package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.CalendarEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for calendar/schedule operations.
 */
interface CalendarRepository {
    fun getAllEntries(): Flow<List<CalendarEntry>>
    fun getEntriesByDay(dayOfWeek: Int): Flow<List<CalendarEntry>>
    fun getWeekSchedule(): Flow<Map<Int, List<CalendarEntry>>>
    suspend fun hasRoutineOnDay(routineId: Long, dayOfWeek: Int): Boolean
    suspend fun insertEntry(entry: CalendarEntry): Long
    suspend fun updateEntry(entry: CalendarEntry)
    suspend fun deleteEntry(entry: CalendarEntry)
    suspend fun deleteEntriesByRoutineId(routineId: Long)
}
