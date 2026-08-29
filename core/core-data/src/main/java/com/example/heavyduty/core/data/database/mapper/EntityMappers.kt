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
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType

// ============ Exercise Mappers ============

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    description = description,
    instructions = instructions,
    imageUrl = imageUrl,
    gifUrl = gifUrl,
    muscleGroup = MuscleGroup.valueOf(muscleGroup),
    exerciseType = ExerciseType.valueOf(exerciseType),
    equipment = equipment,
    isCustom = isCustom,
)

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
)

// ============ Routine Mappers ============

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    name = name,
    description = description,
    type = WorkoutType.valueOf(type),
    programWeekId = programWeekId,
    dayOfWeek = dayOfWeek,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    name = name,
    description = description,
    type = type.name,
    programWeekId = programWeekId,
    dayOfWeek = dayOfWeek,
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

fun RoutineWithExercises.toDomain(): Routine = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    type = WorkoutType.valueOf(routine.type),
    programWeekId = routine.programWeekId,
    dayOfWeek = routine.dayOfWeek,
    createdAt = routine.createdAt,
    updatedAt = routine.updatedAt,
    exercises = routineExercises.map { it.toDomain() },
)

// ============ Program Mappers ============

fun com.example.heavyduty.core.data.database.entity.ProgramEntity.toDomain(weeks: List<com.example.heavyduty.core.domain.model.ProgramWeek> = emptyList()) = com.example.heavyduty.core.domain.model.Program(
    id = id,
    name = name,
    description = description,
    isActive = isActive,
    totalWeeks = totalWeeks,
    createdAt = createdAt,
    updatedAt = updatedAt,
    weeks = weeks,
)

fun com.example.heavyduty.core.domain.model.Program.toEntity() = com.example.heavyduty.core.data.database.entity.ProgramEntity(
    id = id,
    name = name,
    description = description,
    isActive = isActive,
    totalWeeks = totalWeeks,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun com.example.heavyduty.core.data.database.entity.ProgramWeekEntity.toDomain(days: List<Routine> = emptyList()) = com.example.heavyduty.core.domain.model.ProgramWeek(
    id = id,
    programId = programId,
    weekNumber = weekNumber,
    notes = notes,
    days = days,
)

fun com.example.heavyduty.core.domain.model.ProgramWeek.toEntity() = com.example.heavyduty.core.data.database.entity.ProgramWeekEntity(
    id = id,
    programId = programId,
    weekNumber = weekNumber,
    notes = notes,
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

fun com.example.heavyduty.core.domain.model.BodyWeightEntry.toEntity(): com.example.heavyduty.core.data.database.entity.BodyWeightEntity =
    com.example.heavyduty.core.data.database.entity.BodyWeightEntity(
        id = id,
        weightKg = weightKg,
        loggedAt = loggedAt,
    )
