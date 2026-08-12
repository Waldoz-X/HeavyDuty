package com.example.heavyduty.core.data.database

import android.content.Context
import android.util.Log
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * Seed data with 1,324 exercises parsed from JSON dataset.
 * Populated via RoomDatabase.Callback on first database creation.
 */
object SeedData {

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

    suspend fun populateExercises(context: Context, exerciseDao: ExerciseDao) {
        if (exerciseDao.getExerciseCount() > 0) return

        try {
            val inputStream = context.assets.open("exercises.json")
            val reader = InputStreamReader(inputStream)
            val listType = object : TypeToken<List<ExerciseDto>>() {}.type
            val exercisesDto: List<ExerciseDto> = Gson().fromJson(reader, listType)

            val exerciseEntities = exercisesDto.map { dto ->
                // Map the JSON category/body_part to ExerciseType
                val type = when (dto.category?.lowercase()) {
                    "strength", "powerlifting", "strongman", "olympic weightlifting" -> "STRENGTH"
                    "cardio" -> "CARDIO"
                    "stretching", "plyometrics" -> "FUNCTIONAL"
                    else -> "STRENGTH" // Default
                }

                // Map muscle group to MuscleGroup
                val primaryMuscle = dto.body_part?.lowercase() ?: ""
                val muscleGroup = when (primaryMuscle) {
                    "chest" -> "CHEST"
                    "back", "middle back", "lower back", "lats" -> "BACK"
                    "shoulders", "delts" -> "SHOULDERS"
                    "upper arms", "biceps" -> "BICEPS"
                    "triceps" -> "TRICEPS"
                    "lower arms", "forearms" -> "FOREARMS"
                    "upper legs", "quadriceps", "thighs" -> "QUADRICEPS"
                    "hamstrings" -> "HAMSTRINGS"
                    "glutes", "buttocks" -> "GLUTES"
                    "lower legs", "calves" -> "CALVES"
                    "waist", "abdominals" -> "ABS"
                    "neck", "traps" -> "TRAPS"
                    else -> "FULL_BODY"
                }

                val imageUrl = if (!dto.image.isNullOrBlank()) {
                    "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/exercises/${dto.image}"
                } else if (!dto.gif_url.isNullOrBlank()) {
                    "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/exercises/${dto.gif_url}"
                } else {
                    ""
                }

                val instructionsEn = dto.instructions?.get("en") ?: ""

                ExerciseEntity(
                    name = dto.name ?: "Unknown Exercise",
                    muscleGroup = muscleGroup,
                    exerciseType = type,
                    equipment = dto.equipment?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "None",
                    description = "${dto.target?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Full Body"} exercise",
                    instructions = instructionsEn,
                    imageUrl = imageUrl,
                    isCustom = false,
                )
            }

            exerciseDao.insertAllExercises(exerciseEntities)
            Log.d("SeedData", "Successfully inserted ${exerciseEntities.size} exercises from JSON.")
        } catch (e: Exception) {
            Log.e("SeedData", "Error parsing exercises.json", e)
        }
    }
}
