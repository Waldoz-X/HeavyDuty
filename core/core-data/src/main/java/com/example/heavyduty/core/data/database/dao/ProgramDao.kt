package com.example.heavyduty.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.heavyduty.core.data.database.entity.ProgramEntity
import com.example.heavyduty.core.data.database.entity.ProgramWeekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY createdAt DESC")
    fun getAllPrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :programId")
    suspend fun getProgramById(programId: Long): ProgramEntity?

    @Query("SELECT * FROM programs WHERE isActive = 1 LIMIT 1")
    fun getActiveProgram(): Flow<ProgramEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity): Long

    @Update
    suspend fun updateProgram(program: ProgramEntity)

    @Query("DELETE FROM programs WHERE id = :programId")
    suspend fun deleteProgram(programId: Long)

    @Query("UPDATE programs SET isActive = 0")
    suspend fun deactivateAllPrograms()

    @Transaction
    suspend fun setActiveProgram(programId: Long) {
        deactivateAllPrograms()
        val program = getProgramById(programId)
        if (program != null) {
            updateProgram(program.copy(isActive = true))
        }
    }

    // Program Weeks
    @Query("SELECT * FROM program_weeks WHERE programId = :programId ORDER BY weekNumber ASC")
    fun getWeeksForProgram(programId: Long): Flow<List<ProgramWeekEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramWeek(week: ProgramWeekEntity): Long

    @Query("DELETE FROM program_weeks WHERE id = :weekId")
    suspend fun deleteProgramWeek(weekId: Long)
}
