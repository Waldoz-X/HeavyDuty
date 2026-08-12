package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.dao.RoutineDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.data.database.mapper.toEntity
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
) : RoutineRepository {

    override fun getAllRoutines(): Flow<List<Routine>> =
        routineDao.getAllRoutines().map { list -> list.map { it.toDomain() } }

    override fun getRoutineById(id: Long): Flow<Routine?> =
        routineDao.getRoutineById(id).map { it?.toDomain() }

    override fun getRoutineWithExercises(id: Long): Flow<Routine?> =
        routineDao.getRoutineWithExercises(id).map { it?.toDomain() }

    override suspend fun insertRoutine(routine: Routine): Long =
        routineDao.insertRoutine(routine.toEntity())

    override suspend fun updateRoutine(routine: Routine) =
        routineDao.updateRoutine(routine.toEntity())

    override suspend fun deleteRoutine(routine: Routine) =
        routineDao.deleteRoutine(routine.toEntity())

    override suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long =
        routineDao.insertRoutineExercise(routineExercise.toEntity())

    override suspend fun updateRoutineExercise(routineExercise: RoutineExercise) =
        routineDao.updateRoutineExercise(routineExercise.toEntity())

    override suspend fun deleteRoutineExercise(routineExercise: RoutineExercise) =
        routineDao.deleteRoutineExercise(routineExercise.toEntity())

    override suspend fun updateRoutineExerciseOrder(routineId: Long, exercises: List<RoutineExercise>) =
        routineDao.updateRoutineExercises(exercises.map { it.toEntity() })
}
