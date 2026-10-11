package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import com.example.heavyduty.core.data.database.relation.SessionWithSets
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType

/**
 * An unknown workout status reads as cancelled: never counted in the stats as finished training,
 * and never taken for the running workout that the workout screen resumes.
 */
internal val UNKNOWN_STATUS_FALLBACK = WorkoutStatus.CANCELLED

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
