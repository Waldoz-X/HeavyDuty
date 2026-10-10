package com.example.heavyduty.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getExerciseById(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE exerciseType = :type ORDER BY name ASC")
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    // ABORT, not REPLACE: REPLACE deletes the existing row first, which would take the
    // exercise's routine entries with it (and is refused once it has logged sets).
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllExercises(exercises: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 0 AND nameEn = ''")
    suspend fun countUnlocalizedExercises(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 0 AND instructionsEn = '' AND instructions != ''")
    suspend fun countExercisesWithoutEnglishInstructions(): Int

    /**
     * Matched on English name AND Spanish instructions: a few catalog names repeat with
     * different instructions, and the stored row has no other link to its catalog entry.
     */
    @Query(
        "UPDATE exercises SET instructionsEn = :english WHERE isCustom = 0 AND instructionsEn = '' AND nameEn = :nameEn AND instructions = :spanish"
    )
    suspend fun setEnglishInstructions(nameEn: String, spanish: String, english: String)

    /** Catalog rows the catalog couldn't match keep their current instructions, so the fill runs once. */
    @Query("UPDATE exercises SET instructionsEn = instructions WHERE isCustom = 0 AND instructionsEn = ''")
    suspend fun markRemainingInstructionsLocalized()

    /** Fills [setEnglishInstructions] for every catalog entry (nameEn, spanish, english), all or nothing. */
    @Transaction
    suspend fun setEnglishInstructions(entries: List<Triple<String, String, String>>) {
        entries.forEach { (nameEn, spanish, english) -> setEnglishInstructions(nameEn, spanish, english) }
        markRemainingInstructionsLocalized()
    }

    @Query("UPDATE exercises SET name = :spanish, nameEn = :english WHERE isCustom = 0 AND nameEn = '' AND name = :english")
    suspend fun localizeExerciseName(english: String, spanish: String)

    /** Marks catalog rows without a translation as done, keeping their name as-is. */
    @Query("UPDATE exercises SET nameEn = name WHERE isCustom = 0 AND nameEn = ''")
    suspend fun markRemainingExercisesLocalized()

    /** Swaps English catalog names for Spanish ones ([names] = english → spanish), all or nothing. */
    @Transaction
    suspend fun localizeExerciseNames(names: Map<String, String>) {
        names.forEach { (english, spanish) -> localizeExerciseName(english, spanish) }
        markRemainingExercisesLocalized()
    }
}
