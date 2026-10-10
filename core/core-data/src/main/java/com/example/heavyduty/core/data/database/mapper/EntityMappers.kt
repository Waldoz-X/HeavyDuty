package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.CalendarEntryEntity
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import com.example.heavyduty.core.data.database.relation.RoutineWithExercises
import com.example.heavyduty.core.data.database.relation.SessionWithSets
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.Recurrence
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/*
 * Enums are stored by name. Reading one the app doesn't know — a value written by a newer
 * version, a bad import, a hand-edited database — falls back to a safe default instead of
 * throwing, which would crash every screen that lists that row, every time.
 */
internal inline fun <reified E : Enum<E>> String.toEnumOr(default: E): E = enumValues<E>().firstOrNull { it.name == this } ?: default

/**
 * An unknown workout status reads as cancelled: never counted in the stats as finished training,
 * and never taken for the running workout that the workout screen resumes.
 */
internal val UNKNOWN_STATUS_FALLBACK = WorkoutStatus.CANCELLED

// ============ Exercise Mappers ============

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

// ============ Routine Mappers ============

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    name = name,
    description = description,
    type = type.toEnumOr(WorkoutType.GYM),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    name = name,
    description = description,
    type = type.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RoutineExerciseEntity.toDomain(): RoutineExercise = RoutineExercise(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    targetSets = targetSets,
    targetReps = targetReps,
    targetWeight = targetWeight,
    targetDurationSecs = targetDurationSecs,
    restBetweenSetsSecs = restBetweenSetsSecs,
    notes = notes,
    progressionPolicy = progressionPolicy.toEnumOr(ProgressionPolicy.OFF),
    repRangeLow = repRangeLow,
    repRangeHigh = repRangeHigh,
    stallThreshold = stallThreshold,
    supersetGroupId = supersetGroupId,
    weightUnit = weightUnit.toWeightUnit()
)

fun RoutineExercise.toEntity(): RoutineExerciseEntity = RoutineExerciseEntity(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    targetSets = targetSets,
    targetReps = targetReps,
    targetWeight = targetWeight,
    targetDurationSecs = targetDurationSecs,
    restBetweenSetsSecs = restBetweenSetsSecs,
    notes = notes,
    progressionPolicy = progressionPolicy.name,
    repRangeLow = repRangeLow,
    repRangeHigh = repRangeHigh,
    stallThreshold = stallThreshold,
    supersetGroupId = supersetGroupId,
    weightUnit = weightUnit?.name
)

fun com.example.heavyduty.core.data.database.relation.RoutineExerciseWithDetails.toDomain(): RoutineExercise =
    routineExercise.toDomain().copy(
        exercise = exercise.toDomain()
    )

fun RoutineWithExercises.toDomain(): Routine = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    type = routine.type.toEnumOr(WorkoutType.GYM),
    createdAt = routine.createdAt,
    updatedAt = routine.updatedAt,
    // Room returns @Relation rows in storage order, not the user's order — sort explicitly.
    exercises = routineExercises.map { it.toDomain() }.sortedBy { it.orderIndex }
)

// ============ Workout Mappers ============

fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    routineId = routineId,
    sessionName = sessionName,
    startTime = startTime,
    endTime = endTime,
    totalDurationSecs = totalDurationSecs,
    status = status.toEnumOr(UNKNOWN_STATUS_FALLBACK),
    notes = notes,
    type = type.toEnumOr(WorkoutType.GYM)
)

fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    id = id,
    routineId = routineId,
    sessionName = sessionName,
    startTime = startTime,
    endTime = endTime,
    totalDurationSecs = totalDurationSecs,
    status = status.name,
    notes = notes,
    type = type.name
)

fun WorkoutSetEntity.toDomain(): WorkoutSet = WorkoutSet(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    reps = reps,
    weight = weight,
    durationSecs = durationSecs,
    distance = distance,
    isCompleted = isCompleted,
    completedAt = completedAt,
    notes = notes,
    isWarmup = isWarmup,
    effort = effortToDomain(effortType, effortValue),
    weightUnit = weightUnit.toWeightUnit()
)

fun WorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    reps = reps,
    weight = weight,
    durationSecs = durationSecs,
    distance = distance,
    isCompleted = isCompleted,
    completedAt = completedAt,
    notes = notes,
    isWarmup = isWarmup,
    effortType = when (effort) {
        is Effort.Rir -> "RIR"
        is Effort.Rpe -> "RPE"
        null -> null
    },
    effortValue = when (val e = effort) {
        is Effort.Rir -> e.value.toFloat()
        is Effort.Rpe -> e.value
        null -> null
    },
    weightUnit = weightUnit?.name
)

private fun String?.toWeightUnit(): WeightUnit? = this?.let { name -> WeightUnit.entries.firstOrNull { it.name == name } }

private fun effortToDomain(type: String?, value: Float?): Effort? {
    if (type == null || value == null) return null
    return when (type) {
        "RIR" -> Effort.Rir(value.toInt())
        "RPE" -> Effort.Rpe(value)
        else -> null
    }
}

fun com.example.heavyduty.core.data.database.relation.SetWithExercise.toDomain(): WorkoutSet =
    set.toDomain().copy(exercise = exercise.toDomain())

fun SessionWithSets.toDomain(): WorkoutSession = WorkoutSession(
    id = session.id,
    routineId = session.routineId,
    sessionName = session.sessionName,
    startTime = session.startTime,
    endTime = session.endTime,
    totalDurationSecs = session.totalDurationSecs,
    status = session.status.toEnumOr(UNKNOWN_STATUS_FALLBACK),
    notes = session.notes,
    type = session.type.toEnumOr(WorkoutType.GYM),
    sets = sets.map { it.toDomain() }
)

// ============ Calendar Mappers ============

fun CalendarEntryEntity.toDomain(): CalendarEntry = CalendarEntry(
    id = id,
    routineId = routineId,
    dayOfWeek = dayOfWeek,
    recurrence = recurrence.toEnumOr(Recurrence.WEEKLY),
    isActive = isActive
)

fun CalendarEntry.toEntity(): CalendarEntryEntity = CalendarEntryEntity(
    id = id,
    routineId = routineId,
    dayOfWeek = dayOfWeek,
    recurrence = recurrence.name,
    isActive = isActive
)

// ============ Body Weight Mappers ============

fun com.example.heavyduty.core.data.database.entity.BodyWeightEntity.toDomain(): com.example.heavyduty.core.domain.model.BodyWeightEntry =
    com.example.heavyduty.core.domain.model.BodyWeightEntry(
        id = id,
        weightKg = weightKg,
        loggedAt = loggedAt
    )
