package com.example.heavyduty.core.data.database

import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.equipmentLabelFor
import com.google.gson.Gson

// How the bundled catalog's entries (assets/exercises.json) become the exercise rows seeded on
// first launch; SeedData reads the files and writes the rows.

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

/** One catalog entry as the row it's seeded as. */
internal fun SeedData.ExerciseDto.toEntity(spanishNames: Map<String, String>): ExerciseEntity {
    val muscleGroup = muscleGroupOf(target = target, bodyPart = body_part)

    // Stays the raw (title-cased English) catalog value — this is what gets stored
    // as ExerciseEntity.equipment, and Exercise.isBodyweight plus the exercise-list
    // equipment filter both match against it directly. Only the auto-generated
    // description below uses the translated label.
    val equipmentLabel = equipment
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        ?: "None"
    val equipmentLabelEs = (equipment?.let { equipmentLabelFor(it) } ?: "Sin equipo")
        .replaceFirstChar { it.lowercase() }
    val targetLabelEs = (target?.lowercase()?.let { TARGET_LABEL_MAP[it] } ?: "cuerpo completo")
        .replaceFirstChar { it.lowercase() }

    return ExerciseEntity(
        name = name?.let { spanishNames[it] ?: it } ?: "Ejercicio sin nombre",
        nameEn = name.orEmpty(),
        muscleGroup = muscleGroup,
        exerciseType = exerciseTypeOf(category),
        equipment = equipmentLabel,
        description = "Ejercicio con $equipmentLabelEs enfocado en $targetLabelEs.",
        // The dataset ships real, human-translated "es" and "en" instructions (verified:
        // all 1,324 entries have both); Spanish is the default, English shown in English.
        instructions = instructionsIn("es"),
        instructionsEn = instructionsIn("en"),
        imageUrl = mediaUrl(image),
        gifUrl = mediaUrl(gif_url),
        isCustom = false,
        muscleWeightsJson = Gson().toJson(muscleWeights(muscleGroup, secondary_muscles))
    )
}

/**
 * The dataset's `category` field mirrors `body_part` for strength exercises and only
 * diverges for cardio, so it's the only reliable signal for ExerciseType.
 */
internal fun exerciseTypeOf(category: String?): String = when (category?.lowercase()) {
    "cardio" -> "CARDIO"
    "stretching", "plyometrics" -> "FUNCTIONAL"
    else -> "STRENGTH"
}

/**
 * `target` is far more granular than `body_part` (e.g. "upper arms" covers both biceps and
 * triceps), so it's preferred, with body_part as the fallback when it's absent.
 */
internal fun muscleGroupOf(target: String?, bodyPart: String?): String = when (target?.lowercase() ?: "") {
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
    else -> muscleGroupOfBodyPart(bodyPart?.lowercase() ?: "")
}

private fun muscleGroupOfBodyPart(bodyPart: String): String = when (bodyPart) {
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

/**
 * Real secondary-muscle data from the catalog, not a guess — see SECONDARY_MUSCLE_MAP doc.
 * Falls back to crediting the primary alone when none of the listed secondary muscles map
 * to a tracked MuscleGroup.
 */
internal fun muscleWeights(primary: String, secondaryMuscles: List<String>?): Map<String, Float> {
    val secondaryGroups = (secondaryMuscles ?: emptyList())
        .mapNotNull { SECONDARY_MUSCLE_MAP[it.lowercase()] }
        .filter { it != primary }
        .distinct()
    if (secondaryGroups.isEmpty()) return mapOf(primary to 1f)
    val secondaryShare = SECONDARY_MUSCLE_TOTAL_WEIGHT / secondaryGroups.size
    return buildMap {
        put(primary, PRIMARY_MUSCLE_WEIGHT)
        secondaryGroups.forEach { put(it, secondaryShare) }
    }
}

/** The dataset's relative media path as a full URL; none when the entry has no media. */
private fun mediaUrl(path: String?): String = if (!path.isNullOrBlank()) "$RAW_BASE_URL$path" else ""

/**
 * Instructions in [language], one step per line: the step-by-step breakdown when there is
 * one (the detail screen numbers the lines), else the flat paragraph.
 */
internal fun SeedData.ExerciseDto.instructionsIn(language: String): String {
    val steps = instruction_steps?.get(language)
    return if (!steps.isNullOrEmpty()) steps.joinToString("\n") else instructions?.get(language).orEmpty()
}
