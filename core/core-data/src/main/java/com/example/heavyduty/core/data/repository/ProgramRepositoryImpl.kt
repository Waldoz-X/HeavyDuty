package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.common.dispatchers.IoDispatcher
import com.example.heavyduty.core.data.database.dao.ProgramDao
import com.example.heavyduty.core.data.database.dao.RoutineDao
import com.example.heavyduty.core.data.database.mapper.toDomain
import com.example.heavyduty.core.data.database.mapper.toEntity
import com.example.heavyduty.core.domain.model.Program
import com.example.heavyduty.core.domain.model.ProgramWeek
import com.example.heavyduty.core.domain.repository.ProgramRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramRepositoryImpl @Inject constructor(
    private val programDao: ProgramDao,
    private val routineDao: RoutineDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProgramRepository {

    override fun getAllPrograms(): Flow<List<Program>> =
        programDao.getAllPrograms().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getProgramById(programId: Long): Program? = withContext(ioDispatcher) {
        val programEntity = programDao.getProgramById(programId) ?: return@withContext null
        
        // Load weeks and days
        val weeks = programDao.getWeeksForProgram(programId).firstOrNull() ?: emptyList()
        val mappedWeeks = weeks.map { weekEntity ->
            val days = routineDao.getRoutinesForProgramWeek(weekEntity.id).firstOrNull() ?: emptyList()
            weekEntity.toDomain(days = days.map { it.toDomain() })
        }
        
        programEntity.toDomain(weeks = mappedWeeks)
    }

    override fun getActiveProgram(): Flow<Program?> =
        programDao.getActiveProgram().map { it?.toDomain() }

    override suspend fun createProgram(program: Program): Long = withContext(ioDispatcher) {
        programDao.insertProgram(program.toEntity())
    }

    override suspend fun updateProgram(program: Program) = withContext(ioDispatcher) {
        programDao.updateProgram(program.toEntity())
    }

    override suspend fun deleteProgram(programId: Long) = withContext(ioDispatcher) {
        programDao.deleteProgram(programId)
    }

    override suspend fun setActiveProgram(programId: Long) = withContext(ioDispatcher) {
        programDao.setActiveProgram(programId)
    }

    override suspend fun addWeekToProgram(week: ProgramWeek): Long = withContext(ioDispatcher) {
        programDao.insertProgramWeek(week.toEntity())
    }

    override suspend fun duplicateWeek(sourceWeekId: Long, newWeekNumber: Int): Long = withContext(ioDispatcher) {
        // Not implemented fully yet for brevity
        0L
    }
}
