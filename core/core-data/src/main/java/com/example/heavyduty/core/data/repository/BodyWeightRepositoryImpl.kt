package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.BodyWeightDao
import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyWeightRepositoryImpl @Inject constructor(
    private val bodyWeightDao: BodyWeightDao,
) : BodyWeightRepository {

    override fun getAllEntries(): Flow<List<BodyWeightEntry>> =
        bodyWeightDao.getAllEntries().map { list -> list.map { it.toDomain() } }

    override fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<BodyWeightEntry>> =
        bodyWeightDao.getEntriesByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getLatestEntry(): Flow<BodyWeightEntry?> =
        bodyWeightDao.getLatestEntry().map { it?.toDomain() }

    override suspend fun logEntry(weightKg: Float, loggedAt: Long): Long =
        bodyWeightDao.insertEntry(BodyWeightEntity(weightKg = weightKg, loggedAt = loggedAt))

    override suspend fun deleteEntry(entry: BodyWeightEntry) =
        bodyWeightDao.deleteEntry(BodyWeightEntity(id = entry.id, weightKg = entry.weightKg, loggedAt = entry.loggedAt))
}
