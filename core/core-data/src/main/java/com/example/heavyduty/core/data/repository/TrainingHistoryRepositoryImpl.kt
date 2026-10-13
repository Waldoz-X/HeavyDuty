package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TrainingHistoryRepositoryImpl @Inject constructor(private val workoutDao: WorkoutDao) : TrainingHistoryRepository {

    override fun getAllSessions(): Flow<List<WorkoutSession>> = workoutDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> =
        workoutDao.getSessionsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getCompletedSessionsByRoutineId(routineId: Long): Flow<List<WorkoutSession>> =
        workoutDao.getCompletedSessionsByRoutineId(routineId).map { list -> list.map { it.toDomain() } }

    override fun getSetsByExerciseId(exerciseId: Long): Flow<List<WorkoutSet>> =
        workoutDao.getSetsByExerciseId(exerciseId).map { list -> list.map { it.toDomain() } }

    override fun getAllCompletedSets(): Flow<List<WorkoutSet>> = workoutDao.getAllCompletedSets().map { list -> list.map { it.toDomain() } }

    override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSet>> =
        workoutDao.getCompletedSetsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }
}
