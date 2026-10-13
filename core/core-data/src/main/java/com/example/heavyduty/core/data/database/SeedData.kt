package com.example.heavyduty.core.data.database

import android.content.Context
import android.util.Log
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlinx.coroutines.CancellationException

/**
 * Seed data with 1,324 exercises parsed from JSON dataset.
 * Populated via RoomDatabase.Callback on first database creation.
 *
 * Every step catches any exception and logs it: it runs as the database opens, and a bad
 * asset or a failed update must leave the app usable (with fewer exercises) rather than crash
 * it on start.
 */
@Suppress("TooGenericExceptionCaught")
object SeedData {

    private const val SPANISH_NAMES_ASSET = "exercise_names_es.json"

    // Named like exercises.json's keys, which Gson reads them by.
    @Suppress("ConstructorParameterNaming")
    data class ExerciseDto(
        val id: String?,
        val name: String?,
        val category: String?,
        val body_part: String?,
        val equipment: String?,
        val instructions: Map<String, String>?,
        val instruction_steps: Map<String, List<String>>?,
        val muscle_group: String?,
        val secondary_muscles: List<String>?,
        val target: String?,
        val image: String?,
        val gif_url: String?,
        val media_id: String?
    )

    /** english catalog name → Spanish display name (assets/exercise_names_es.json). */
    private fun loadSpanishNames(context: Context): Map<String, String> = try {
        context.assets.open(SPANISH_NAMES_ASSET).use { stream ->
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson<Map<String, String>>(InputStreamReader(stream), type).orEmpty()
        }
    } catch (e: Exception) {
        Log.e("SeedData", "Error reading $SPANISH_NAMES_ASSET", e)
        emptyMap()
    }

    /**
     * Gives installs seeded before the catalog was translated their Spanish names (and records
     * the English one in `nameEn`). Runs on every open; a single COUNT once everything is done.
     */
    suspend fun localizeExerciseNames(context: Context, exerciseDao: ExerciseDao) {
        try {
            if (exerciseDao.countUnlocalizedExercises() == 0) return
            val names = loadSpanishNames(context)
            if (names.isEmpty()) return
            exerciseDao.localizeExerciseNames(names)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SeedData", "Error localizing exercise names", e)
        }
    }

    /**
     * Gives installs seeded before English instructions existed (DB < 17) their English
     * instructions. Runs on every open; a single COUNT once everything is filled.
     */
    suspend fun localizeExerciseInstructions(context: Context, exerciseDao: ExerciseDao) {
        try {
            if (exerciseDao.countExercisesWithoutEnglishInstructions() == 0) return
            val entries = loadCatalog(context).mapNotNull { dto ->
                val nameEn = dto.name ?: return@mapNotNull null
                val english = dto.instructionsIn("en").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Triple(nameEn, dto.instructionsIn("es"), english)
            }
            exerciseDao.setEnglishInstructions(entries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SeedData", "Error adding English instructions", e)
        }
    }

    private fun loadCatalog(context: Context): List<ExerciseDto> = context.assets.open("exercises.json").use { stream ->
        val listType = object : TypeToken<List<ExerciseDto>>() {}.type
        Gson().fromJson(InputStreamReader(stream), listType)
    }

    suspend fun populateExercises(context: Context, exerciseDao: ExerciseDao) {
        if (exerciseDao.getExerciseCount() > 0) return
        val spanishNames = loadSpanishNames(context)

        try {
            val exerciseEntities = loadCatalog(context).map { it.toEntity(spanishNames) }
            exerciseDao.insertAllExercises(exerciseEntities)
            Log.d("SeedData", "Successfully inserted ${exerciseEntities.size} exercises from JSON.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SeedData", "Error parsing exercises.json", e)
        }
    }
}
