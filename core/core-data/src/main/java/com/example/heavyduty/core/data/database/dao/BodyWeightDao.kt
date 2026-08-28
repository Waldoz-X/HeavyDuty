package com.example.heavyduty.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries ORDER BY loggedAt ASC")
    fun getAllEntries(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight_entries WHERE loggedAt BETWEEN :startTime AND :endTime ORDER BY loggedAt ASC")
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight_entries ORDER BY loggedAt DESC LIMIT 1")
    fun getLatestEntry(): Flow<BodyWeightEntity?>

    @Insert
    suspend fun insertEntry(entry: BodyWeightEntity): Long

    @Delete
    suspend fun deleteEntry(entry: BodyWeightEntity)
}
