package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.relation.RoutineWithExercises
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutType

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
