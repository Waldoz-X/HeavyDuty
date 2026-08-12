package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for routine operations.
 */
interface RoutineRepository {
    fun getAllRoutines(): Flow<List<Routine>>
    fun getRoutineById(id: Long): Flow<Routine?>
    fun getRoutineWithExercises(id: Long): Flow<Routine?>
    suspend fun insertRoutine(routine: Routine): Long
    suspend fun updateRoutine(routine: Routine)
    suspend fun deleteRoutine(routine: Routine)
    suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long
    suspend fun updateRoutineExercise(routineExercise: RoutineExercise)
    suspend fun deleteRoutineExercise(routineExercise: RoutineExercise)
    suspend fun updateRoutineExerciseOrder(routineId: Long, exercises: List<RoutineExercise>)
}
