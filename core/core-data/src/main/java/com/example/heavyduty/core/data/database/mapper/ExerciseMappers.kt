package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WeightUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun ExerciseEntity.toDomain(): Exercise {
    val primary = muscleGroup.toEnumOr(MuscleGroup.FULL_BODY)
    return Exercise(
        id = id,
        name = name,
        nameEn = nameEn,
        description = description,
        instructions = instructions,
        instructionsEn = instructionsEn,
        imageUrl = imageUrl,
        gifUrl = gifUrl,
        muscleGroup = primary,
        exerciseType = exerciseType.toEnumOr(ExerciseType.STRENGTH),
        equipment = equipment,
        isCustom = isCustom,
        weightUnit = weightUnit.toEnumOr(WeightUnit.KG),
        muscleWeights = decodeMuscleWeights(muscleWeightsJson, primary)
    )
}

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    nameEn = nameEn,
    description = description,
    instructions = instructions,
    instructionsEn = instructionsEn,
    imageUrl = imageUrl,
    gifUrl = gifUrl,
    muscleGroup = muscleGroup.name,
    exerciseType = exerciseType.name,
    equipment = equipment,
    isCustom = isCustom,
    weightUnit = weightUnit.name,
    muscleWeightsJson = encodeMuscleWeights(muscleWeights)
)

private val muscleWeightsGson = Gson()
private val muscleWeightsMapType = object : TypeToken<Map<String, Float>>() {}.type

/** Falls back to crediting [primary] alone on blank/unparseable JSON. */
private fun decodeMuscleWeights(json: String, primary: MuscleGroup): Map<MuscleGroup, Float> {
    if (json.isBlank()) return mapOf(primary to 1f)
    return try {
        val raw: Map<String, Float>? = muscleWeightsGson.fromJson(json, muscleWeightsMapType)
        raw.orEmpty()
            .mapNotNull { (key, weight) -> MuscleGroup.entries.firstOrNull { it.name == key }?.let { it to weight } }
            .toMap()
            .ifEmpty { mapOf(primary to 1f) }
    } catch (_: Exception) {
        mapOf(primary to 1f)
    }
}

private fun encodeMuscleWeights(weights: Map<MuscleGroup, Float>): String = muscleWeightsGson.toJson(weights.mapKeys { it.key.name })
