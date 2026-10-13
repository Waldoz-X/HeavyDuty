package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.data.database.mapper.toEntity
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WorkoutRepositoryImpl @Inject constructor(private val workoutDao: WorkoutDao) : WorkoutRepository {

    override fun getSessionById(id: Long): Flow<WorkoutSession?> = workoutDao.getSessionById(id).map { it?.toDomain() }

    override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> = workoutDao.getSessionWithSets(id).map { it?.toDomain() }

    override fun getActiveSession(): Flow<WorkoutSession?> = workoutDao.getActiveSession().map { it?.toDomain() }

    override suspend fun insertSession(session: WorkoutSession): Long = workoutDao.insertSession(session.toEntity())

    override suspend fun updateSession(session: WorkoutSession) = workoutDao.updateSession(session.toEntity())

    override suspend fun deleteSession(session: WorkoutSession) = workoutDao.deleteSession(session.toEntity())

    override suspend fun insertSet(set: WorkoutSet): Long = workoutDao.insertSet(set.toEntity())

    override suspend fun updateSet(set: WorkoutSet) = workoutDao.updateSet(set.toEntity())

    override suspend fun deleteSet(set: WorkoutSet) = workoutDao.deleteSet(set.toEntity())
}
