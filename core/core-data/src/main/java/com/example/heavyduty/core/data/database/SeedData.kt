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
        "lats" to "LATS"
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
        "upper back" to "Espalda alta"
    )

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

    /**
     * Instructions in [language], one step per line: the step-by-step breakdown when there is
     * one (the detail screen numbers the lines), else the flat paragraph.
     */
    private fun ExerciseDto.instructionsIn(language: String): String {
        val steps = instruction_steps?.get(language)
        return if (!steps.isNullOrEmpty()) steps.joinToString("\n") else instructions?.get(language).orEmpty()
    }

    private fun loadCatalog(context: Context): List<ExerciseDto> = context.assets.open("exercises.json").use { stream ->
        val listType = object : TypeToken<List<ExerciseDto>>() {}.type
        Gson().fromJson(InputStreamReader(stream), listType)
    }

    suspend fun populateExercises(context: Context, exerciseDao: ExerciseDao) {
        if (exerciseDao.getExerciseCount() > 0) return
        val spanishNames = loadSpanishNames(context)

        try {
            val exercisesDto: List<ExerciseDto> = loadCatalog(context)

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

                // The dataset ships real, human-translated "es" and "en" instructions (verified:
                // all 1,324 entries have both); Spanish is the default, English shown in English.
                val instructionsEs = dto.instructionsIn("es")
                val instructionsEn = dto.instructionsIn("en")

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
                    name = dto.name?.let { spanishNames[it] ?: it } ?: "Ejercicio sin nombre",
                    nameEn = dto.name.orEmpty(),
                    muscleGroup = muscleGroup,
                    exerciseType = type,
                    equipment = equipmentLabel,
                    description = "Ejercicio con $equipmentLabelEs enfocado en $targetLabelEs.",
                    instructions = instructionsEs,
                    instructionsEn = instructionsEn,
                    imageUrl = imageUrl,
                    gifUrl = gifUrl,
                    isCustom = false,
                    muscleWeightsJson = Gson().toJson(muscleWeights)
                )
            }

            exerciseDao.insertAllExercises(exerciseEntities)
            Log.d("SeedData", "Successfully inserted ${exerciseEntities.size} exercises from JSON.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SeedData", "Error parsing exercises.json", e)
        }
    }
}
