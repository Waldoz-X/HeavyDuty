package com.example.heavyduty.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.heavyduty.core.data.database.entity.CalendarEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    @Query("SELECT * FROM calendar_entries WHERE isActive = 1 ORDER BY dayOfWeek ASC")
    fun getAllEntries(): Flow<List<CalendarEntryEntity>>

    @Query("SELECT * FROM calendar_entries WHERE dayOfWeek = :dayOfWeek AND isActive = 1")
    fun getEntriesByDay(dayOfWeek: Int): Flow<List<CalendarEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CalendarEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: CalendarEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: CalendarEntryEntity)

    @Query("DELETE FROM calendar_entries WHERE routineId = :routineId")
    suspend fun deleteEntriesByRoutineId(routineId: Long)
}
