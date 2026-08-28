package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.BodyWeightEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for body-weight log entries.
 */
interface BodyWeightRepository {
    fun getAllEntries(): Flow<List<BodyWeightEntry>>
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<BodyWeightEntry>>
    fun getLatestEntry(): Flow<BodyWeightEntry?>
    suspend fun logEntry(weightKg: Float, loggedAt: Long = System.currentTimeMillis()): Long
    suspend fun deleteEntry(entry: BodyWeightEntry)
}
