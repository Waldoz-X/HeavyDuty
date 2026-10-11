package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import com.example.heavyduty.core.data.database.relation.RoutineExerciseWithDetails
import com.example.heavyduty.core.data.database.relation.RoutineWithExercises

// ---------------------------------------------------------------- database rows → backup

internal fun ExerciseEntity.toDto() = ExerciseDto(
    name = name,
    description = description,
    instructions = instructions,
    muscleGroup = muscleGroup,
    exerciseType = exerciseType,
    equipment = equipment,
    muscleWeightsJson = muscleWeightsJson
)

internal fun RoutineWithExercises.toDto() = RoutineDto(
    name = routine.name,
    description = routine.description,
    type = routine.type,
    createdAt = routine.createdAt,
    updatedAt = routine.updatedAt,
    exercises = routineExercises.map { it.toDto() }
)

private fun RoutineExerciseWithDetails.toDto() = RoutineExerciseDto(
    // Same stable key as sets: the English catalog name, so a re-translated
    // catalog still resolves it (it used to export the Spanish display name).
    exerciseName = exercise.nameEn.ifBlank { exercise.name },
    orderIndex = routineExercise.orderIndex,
    targetSets = routineExercise.targetSets,
    targetReps = routineExercise.targetReps,
    targetWeight = routineExercise.targetWeight,
    targetDurationSecs = routineExercise.targetDurationSecs,
    restBetweenSetsSecs = routineExercise.restBetweenSetsSecs,
    notes = routineExercise.notes,
    progressionPolicy = routineExercise.progressionPolicy,
    repRangeLow = routineExercise.repRangeLow,
    repRangeHigh = routineExercise.repRangeHigh,
    stallThreshold = routineExercise.stallThreshold,
    supersetGroupId = routineExercise.supersetGroupId,
    weightUnit = routineExercise.weightUnit
)

/** Sets whose exercise has no name to export by (see [exerciseNameById]) are left out. */
internal fun WorkoutSessionEntity.toDto(sets: List<WorkoutSetEntity>, exerciseNameById: Map<Long, String>) = SessionDto(
    sessionName = sessionName,
    startTime = startTime,
    endTime = endTime,
    totalDurationSecs = totalDurationSecs,
    status = status,
    notes = notes,
    type = type,
    sets = sets.mapNotNull { set -> exerciseNameById[set.exerciseId]?.let { set.toDto(exerciseName = it) } }
)

private fun WorkoutSetEntity.toDto(exerciseName: String) = SetDto(
    exerciseName = exerciseName,
    setNumber = setNumber,
    reps = reps,
    weight = weight,
    durationSecs = durationSecs,
    distance = distance,
    isCompleted = isCompleted,
    completedAt = completedAt,
    notes = notes,
    isWarmup = isWarmup,
    effortType = effortType,
    effortValue = effortValue,
    weightUnit = weightUnit
)

// ---------------------------------------------------------------- backup → database rows
// Each missing optional field falls back to the app's default; required ones were checked by validate().

internal fun ExerciseDto.toEntity() = ExerciseEntity(
    name = requireNotNull(name),
    description = description.orEmpty(),
    instructions = instructions.orEmpty(),
    muscleGroup = requireNotNull(muscleGroup),
    exerciseType = requireNotNull(exerciseType),
    equipment = equipment.orEmpty(),
    isCustom = true,
    muscleWeightsJson = muscleWeightsJson.orEmpty()
)

internal fun RoutineDto.toEntity(createdAt: Long, now: Long) = RoutineEntity(
    name = requireNotNull(name),
    description = description.orEmpty(),
    type = type ?: "GYM",
    createdAt = createdAt,
    updatedAt = updatedAt ?: now
)

/** Linked to its routine once the routine row exists. */
internal fun RoutineExerciseDto.toEntity(exerciseId: Long) = RoutineExerciseEntity(
    routineId = 0,
    exerciseId = exerciseId,
    orderIndex = orderIndex ?: 0,
    targetSets = targetSets ?: 3,
    targetReps = targetReps ?: 10,
    targetWeight = targetWeight ?: 0f,
    targetDurationSecs = targetDurationSecs ?: 0,
    restBetweenSetsSecs = restBetweenSetsSecs ?: 60,
    notes = notes.orEmpty(),
    progressionPolicy = progressionPolicy ?: "LINEAR",
    repRangeLow = repRangeLow ?: 8,
    repRangeHigh = repRangeHigh ?: 10,
    stallThreshold = stallThreshold ?: 3,
    supersetGroupId = supersetGroupId,
    weightUnit = weightUnit
)

/**
 * A workout still running when the backup was made comes back finished at its last set:
 * imported as running, it would take over the workout screen on this install.
 */
internal fun SessionDto.toEntity(): WorkoutSessionEntity {
    val start = requireNotNull(startTime)
    val session = WorkoutSessionEntity(
        routineId = null,
        sessionName = sessionName.orEmpty(),
        startTime = start,
        endTime = endTime,
        totalDurationSecs = totalDurationSecs ?: 0,
        status = status ?: STATUS_COMPLETED,
        notes = notes.orEmpty(),
        type = type ?: "GYM"
    )
    if (status != STATUS_IN_PROGRESS) return session
    val end = sets.orEmpty().mapNotNull { it.completedAt }.maxOrNull() ?: start
    return session.copy(
        endTime = end,
        totalDurationSecs = ((end - start) / MILLIS_PER_SECOND).toInt().coerceAtLeast(0),
        status = STATUS_COMPLETED
    )
}

internal fun SetDto.toEntity(sessionId: Long, exerciseId: Long) = WorkoutSetEntity(
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber ?: 1,
    reps = reps,
    weight = weight,
    durationSecs = durationSecs,
    distance = distance,
    isCompleted = isCompleted ?: true,
    completedAt = completedAt,
    notes = notes.orEmpty(),
    isWarmup = isWarmup ?: false,
    effortType = effortType,
    effortValue = effortValue,
    weightUnit = weightUnit
)

private const val MILLIS_PER_SECOND = 1_000L
