package com.example.heavyduty.core.data.database

import android.content.Context
import android.util.Log
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.equipmentLabelFor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * Seed data with 1,324 exercises parsed from JSON dataset.
 * Populated via RoomDatabase.Callback on first database creation.
 */
object SeedData {

    private const val RAW_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/"

    /** Primary muscle's share of an exercise's fractional credit; secondaries split the rest. */
    private const val PRIMARY_MUSCLE_WEIGHT = 0.6f
    private const val SECONDARY_MUSCLE_TOTAL_WEIGHT = 0.4f

    /**
     * Maps the dataset's free-text `secondary_muscles` values (40 distinct strings across the
     * catalog) to this app's [MuscleGroup] taxonomy. A handful of minor/untracked areas (wrists,
     * hands, feet, ankles, neck-front) have no matching bucket and are intentionally omitted
     * rather than forced into a wrong one — the same granularity limit the primary-muscle
     * mapping below already accepts.
     */
    private val SECONDARY_MUSCLE_MAP = mapOf(
        "shoulders" to "SHOULDERS",
        "deltoids" to "SHOULDERS",
        "rear deltoids" to "SHOULDERS",
        "rotator cuff" to "SHOULDERS",
        "hamstrings" to "HAMSTRINGS",
        "forearms" to "FOREARMS",
        "wrist flexors" to "FOREARMS",
        "wrist extensors" to "FOREARMS",
        "grip muscles" to "FOREARMS",
        "triceps" to "TRICEPS",
        "biceps" to "BICEPS",
        "brachialis" to "BICEPS",
        "quadriceps" to "QUADRICEPS",
        "hip flexors" to "QUADRICEPS",
        "groin" to "QUADRICEPS",
        "inner thighs" to "QUADRICEPS",
        "calves" to "CALVES",
        "soleus" to "CALVES",
        "shins" to "CALVES",
        "glutes" to "GLUTES",
        "core" to "ABS",
        "abdominals" to "ABS",
        "lower abs" to "ABS",
        "chest" to "CHEST",
        "upper chest" to "CHEST",
        "obliques" to "OBLIQUES",
        "lower back" to "BACK",
        "rhomboids" to "BACK",
        "upper back" to "BACK",
        "back" to "BACK",
        "trapezius" to "TRAPS",
        "traps" to "TRAPS",
        "latissimus dorsi" to "LATS",
        "lats" to "LATS",
    )

    /**
     * Spanish label for the dataset's `target` values (19 distinct strings across the catalog).
     * Only used to build [ExerciseEntity.description] below — `target` itself isn't a stored
     * field, unlike `equipment` (see [com.example.heavyduty.core.domain.model.equipmentLabelFor]
     * for why that one lives in core-domain instead of here).
     */
    private val TARGET_LABEL_MAP = mapOf(
        "abductors" to "Abductores",
        "abs" to "Abdominales",
        "adductors" to "Aductores",
        "biceps" to "Bíceps",
        "calves" to "Pantorrillas",
        "cardiovascular system" to "Sistema cardiovascular",
        "delts" to "Deltoides",
        "forearms" to "Antebrazos",
        "glutes" to "Glúteos",
        "hamstrings" to "Isquiotibiales",
        "lats" to "Dorsales",
        "levator scapulae" to "Elevador de la escápula",
        "pectorals" to "Pectorales",
        "quads" to "Cuádriceps",
        "serratus anterior" to "Serrato anterior",
        "spine" to "Columna",
        "traps" to "Trapecios",
        "triceps" to "Tríceps",
        "upper back" to "Espalda alta",
    )

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
                // so the detail screen can render a numbered list. The dataset ships a real,
                // human-translated "es" key for both fields (verified: all 1,324 entries have
                // one) — it was reading "en" before, showing English instructions everywhere.
                val instructionSteps = dto.instruction_steps?.get("es")
                val instructionsEs = if (!instructionSteps.isNullOrEmpty()) {
                    instructionSteps.joinToString("\n")
                } else {
                    dto.instructions?.get("es") ?: ""
                }

                // Stays the raw (title-cased English) catalog value — this is what gets stored
                // as ExerciseEntity.equipment, and Exercise.isBodyweight plus the exercise-list
                // equipment filter both match against it directly. Only the auto-generated
                // description below uses the translated label.
                val equipmentLabel = dto.equipment
                    ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    ?: "None"
                val equipmentLabelEs = (dto.equipment?.let { equipmentLabelFor(it) } ?: "Sin equipo")
                    .replaceFirstChar { it.lowercase() }
                val targetLabelEs = (dto.target?.lowercase()?.let { TARGET_LABEL_MAP[it] } ?: "cuerpo completo")
                    .replaceFirstChar { it.lowercase() }

                // Real secondary-muscle data from the catalog, not a guess — see
                // SECONDARY_MUSCLE_MAP doc. Falls back to crediting the primary alone when
                // none of the listed secondary muscles map to a tracked MuscleGroup.
                val secondaryGroups = (dto.secondary_muscles ?: emptyList())
                    .mapNotNull { SECONDARY_MUSCLE_MAP[it.lowercase()] }
                    .filter { it != muscleGroup }
                    .distinct()
                val muscleWeights = if (secondaryGroups.isEmpty()) {
                    mapOf(muscleGroup to 1f)
                } else {
                    val secondaryShare = SECONDARY_MUSCLE_TOTAL_WEIGHT / secondaryGroups.size
                    buildMap {
                        put(muscleGroup, PRIMARY_MUSCLE_WEIGHT)
                        secondaryGroups.forEach { put(it, secondaryShare) }
                    }
                }

                ExerciseEntity(
                    name = dto.name ?: "Ejercicio sin nombre",
                    muscleGroup = muscleGroup,
                    exerciseType = type,
                    equipment = equipmentLabel,
                    description = "Ejercicio con $equipmentLabelEs enfocado en $targetLabelEs.",
                    instructions = instructionsEs,
                    imageUrl = imageUrl,
                    gifUrl = gifUrl,
                    isCustom = false,
                    muscleWeightsJson = Gson().toJson(muscleWeights),
                )
            }

            exerciseDao.insertAllExercises(exerciseEntities)
            Log.d("SeedData", "Successfully inserted ${exerciseEntities.size} exercises from JSON.")
        } catch (e: Exception) {
            Log.e("SeedData", "Error parsing exercises.json", e)
        }
    }
}
