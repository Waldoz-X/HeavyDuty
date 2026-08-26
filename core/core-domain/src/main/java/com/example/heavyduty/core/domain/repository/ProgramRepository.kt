package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.Program
import com.example.heavyduty.core.domain.model.ProgramWeek
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun getAllPrograms(): Flow<List<Program>>
    suspend fun getProgramById(programId: Long): Program?
    fun getActiveProgram(): Flow<Program?>
    
    suspend fun createProgram(program: Program): Long
    suspend fun updateProgram(program: Program)
    suspend fun deleteProgram(programId: Long)
    
    suspend fun setActiveProgram(programId: Long)
    
    suspend fun addWeekToProgram(week: ProgramWeek): Long
    suspend fun duplicateWeek(sourceWeekId: Long, newWeekNumber: Int): Long
}
