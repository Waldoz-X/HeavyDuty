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

    private const val RAW_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/"

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
                // The dataset's `category` field mirrors `body_part` for strength exercises and
                // only diverges for cardio, so it's the only reliable signal for ExerciseType.
                val type = when (dto.category?.lowercase()) {
                    "cardio" -> "CARDIO"
                    "stretching", "plyometrics" -> "FUNCTIONAL"
                    else -> "STRENGTH"
                }

                // `target` is far more granular than `body_part` (e.g. "upper arms" covers both
                // biceps and triceps), so prefer it and fall back to body_part when absent.
                val target = dto.target?.lowercase() ?: ""
                val bodyPart = dto.body_part?.lowercase() ?: ""
                val muscleGroup = when (target) {
                    "abs" -> "ABS"
                    "pectorals", "serratus anterior" -> "CHEST"
                    "biceps" -> "BICEPS"
                    "triceps" -> "TRICEPS"
                    "delts" -> "SHOULDERS"
                    "upper back", "spine" -> "BACK"
                    "lats" -> "LATS"
                    "calves" -> "CALVES"
                    "quads", "adductors", "abductors" -> "QUADRICEPS"
                    "hamstrings" -> "HAMSTRINGS"
                    "glutes" -> "GLUTES"
                    "forearms" -> "FOREARMS"
                    "traps", "levator scapulae" -> "TRAPS"
                    "cardiovascular system" -> "CARDIO"
                    else -> when (bodyPart) {
                        "chest" -> "CHEST"
                        "back" -> "BACK"
                        "shoulders" -> "SHOULDERS"
                        "upper legs" -> "QUADRICEPS"
                        "lower legs" -> "CALVES"
                        "upper arms" -> "BICEPS"
                        "lower arms" -> "FOREARMS"
                        "waist" -> "ABS"
                        "cardio" -> "CARDIO"
                        "neck" -> "TRAPS"
                        else -> "FULL_BODY"
                    }
                }

                val imageUrl = if (!dto.image.isNullOrBlank()) {
                    "$RAW_BASE_URL${dto.image}"
                } else {
                    ""
                }
                val gifUrl = if (!dto.gif_url.isNullOrBlank()) {
                    "$RAW_BASE_URL${dto.gif_url}"
                } else {
                    ""
                }

                // Prefer the step-by-step breakdown (one step per line) over the flat paragraph
                // so the detail screen can render a numbered list.
                val instructionSteps = dto.instruction_steps?.get("en")
                val instructionsEn = if (!instructionSteps.isNullOrEmpty()) {
                    instructionSteps.joinToString("\n")
                } else {
                    dto.instructions?.get("en") ?: ""
                }

                val equipmentLabel = dto.equipment
                    ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    ?: "None"
                val targetLabel = dto.target
                    ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    ?: "Full Body"

                val article = if (equipmentLabel.firstOrNull()?.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')) "An" else "A"

                ExerciseEntity(
                    name = dto.name ?: "Unknown Exercise",
                    muscleGroup = muscleGroup,
                    exerciseType = type,
                    equipment = equipmentLabel,
                    description = "$article $equipmentLabel exercise targeting the $targetLabel.",
                    instructions = instructionsEn,
                    imageUrl = imageUrl,
                    gifUrl = gifUrl,
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
