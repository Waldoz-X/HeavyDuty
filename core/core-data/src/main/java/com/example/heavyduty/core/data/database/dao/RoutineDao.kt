package com.example.heavyduty.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.relation.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Transaction
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineWithExercises>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineById(id: Long): Flow<RoutineEntity?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineWithExercises(id: Long): Flow<RoutineWithExercises?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    // Routine Exercise operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(routineExercise: RoutineExerciseEntity): Long

    @Update
    suspend fun updateRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Delete
    suspend fun deleteRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Update
    suspend fun updateRoutineExercises(routineExercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesByRoutineId(routineId: Long)

    /** Inserts a routine with its exercises atomically; returns the new routine id. */
    @Transaction
    suspend fun insertRoutineWithExercises(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>): Long {
        val routineId = insertRoutine(routine)
        exercises.forEach { insertRoutineExercise(it.copy(routineId = routineId)) }
        return routineId
    }

    /**
     * Replaces a routine's exercise list atomically, so a failure halfway through can never
     * leave the routine with only some of its exercises.
     */
    @Transaction
    suspend fun replaceRoutineWithExercises(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>) {
        updateRoutine(routine)
        deleteExercisesByRoutineId(routine.id)
        exercises.forEach { insertRoutineExercise(it.copy(routineId = routine.id)) }
    }
}
