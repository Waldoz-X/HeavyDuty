package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.CalendarDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.data.database.mapper.toEntity
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val calendarDao: CalendarDao,
) : CalendarRepository {

    override fun getAllEntries(): Flow<List<CalendarEntry>> =
        calendarDao.getAllEntries().map { list -> list.map { it.toDomain() } }

    override fun getEntriesByDay(dayOfWeek: Int): Flow<List<CalendarEntry>> =
        calendarDao.getEntriesByDay(dayOfWeek).map { list -> list.map { it.toDomain() } }

    override fun getWeekSchedule(): Flow<Map<Int, List<CalendarEntry>>> =
        calendarDao.getAllEntries().map { list ->
            list.map { it.toDomain() }.groupBy { it.dayOfWeek }
        }

    override suspend fun insertEntry(entry: CalendarEntry): Long =
        calendarDao.insertEntry(entry.toEntity())

    override suspend fun updateEntry(entry: CalendarEntry) =
        calendarDao.updateEntry(entry.toEntity())

    override suspend fun deleteEntry(entry: CalendarEntry) =
        calendarDao.deleteEntry(entry.toEntity())

    override suspend fun deleteEntriesByRoutineId(routineId: Long) =
        calendarDao.deleteEntriesByRoutineId(routineId)
}
