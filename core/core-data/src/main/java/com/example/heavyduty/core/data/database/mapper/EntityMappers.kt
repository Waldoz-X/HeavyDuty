package com.example.heavyduty.core.data.database.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType

// ============ Exercise Mappers ============

fun ExerciseEntity.toDomain(): Exercise {
    val primary = MuscleGroup.valueOf(muscleGroup)
    return Exercise(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        imageUrl = imageUrl,
        gifUrl = gifUrl,
        muscleGroup = primary,
        exerciseType = ExerciseType.valueOf(exerciseType),
        equipment = equipment,
        isCustom = isCustom,
        weightUnit = runCatching { WeightUnit.valueOf(weightUnit) }.getOrDefault(WeightUnit.KG),
        muscleWeights = decodeMuscleWeights(muscleWeightsJson, primary),
    )
}

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    description = description,
    instructions = instructions,
    imageUrl = imageUrl,
    gifUrl = gifUrl,
    muscleGroup = muscleGroup.name,
    exerciseType = exerciseType.name,
    equipment = equipment,
    isCustom = isCustom,
    weightUnit = weightUnit.name,
    muscleWeightsJson = encodeMuscleWeights(muscleWeights),
)

private val muscleWeightsGson = Gson()
private val muscleWeightsMapType = object : TypeToken<Map<String, Float>>() {}.type

/** Falls back to crediting [primary] alone on blank/unparseable JSON. */
private fun decodeMuscleWeights(json: String, primary: MuscleGroup): Map<MuscleGroup, Float> {
    if (json.isBlank()) return mapOf(primary to 1f)
    return try {
        val raw: Map<String, Float>? = muscleWeightsGson.fromJson(json, muscleWeightsMapType)
        raw.orEmpty()
            .mapNotNull { (key, weight) -> runCatching { MuscleGroup.valueOf(key) }.getOrNull()?.let { it to weight } }
            .toMap()
            .ifEmpty { mapOf(primary to 1f) }
    } catch (_: Exception) {
        mapOf(primary to 1f)
    }
}

private fun encodeMuscleWeights(weights: Map<MuscleGroup, Float>): String =
    muscleWeightsGson.toJson(weights.mapKeys { it.key.name })

// ============ Routine Mappers ============

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    name = name,
    description = description,
    type = WorkoutType.valueOf(type),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    name = name,
    description = description,
    type = type.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
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
    progressionPolicy = ProgressionPolicy.valueOf(progressionPolicy),
    repRangeLow = repRangeLow,
    repRangeHigh = repRangeHigh,
    stallThreshold = stallThreshold,
    supersetGroupId = supersetGroupId,
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
)

fun com.example.heavyduty.core.data.database.relation.RoutineExerciseWithDetails.toDomain(): RoutineExercise = routineExercise.toDomain().copy(
    exercise = exercise.toDomain()
)

fun RoutineWithExercises.toDomain(): Routine = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    type = WorkoutType.valueOf(routine.type),
    createdAt = routine.createdAt,
    updatedAt = routine.updatedAt,
    exercises = routineExercises.map { it.toDomain() },
)

// ============ Workout Mappers ============

fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    routineId = routineId,
    sessionName = sessionName,
    startTime = startTime,
    endTime = endTime,
    totalDurationSecs = totalDurationSecs,
    status = WorkoutStatus.valueOf(status),
    notes = notes,
    type = WorkoutType.valueOf(type),
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
    type = type.name,
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
)

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
    status = WorkoutStatus.valueOf(session.status),
    notes = session.notes,
    type = WorkoutType.valueOf(session.type),
    sets = sets.map { it.toDomain() },
)

// ============ Calendar Mappers ============

fun CalendarEntryEntity.toDomain(): CalendarEntry = CalendarEntry(
    id = id,
    routineId = routineId,
    dayOfWeek = dayOfWeek,
    recurrence = Recurrence.valueOf(recurrence),
    isActive = isActive,
)

fun CalendarEntry.toEntity(): CalendarEntryEntity = CalendarEntryEntity(
    id = id,
    routineId = routineId,
    dayOfWeek = dayOfWeek,
    recurrence = recurrence.name,
    isActive = isActive,
)

// ============ Body Weight Mappers ============

fun com.example.heavyduty.core.data.database.entity.BodyWeightEntity.toDomain(): com.example.heavyduty.core.domain.model.BodyWeightEntry =
    com.example.heavyduty.core.domain.model.BodyWeightEntry(
        id = id,
        weightKg = weightKg,
        loggedAt = loggedAt,
    )

