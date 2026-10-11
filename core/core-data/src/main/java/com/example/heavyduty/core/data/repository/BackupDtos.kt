package com.example.heavyduty.core.data.repository

import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType

internal const val BACKUP_FORMAT_VERSION = 1
internal const val STATUS_IN_PROGRESS = "IN_PROGRESS"
internal const val STATUS_COMPLETED = "COMPLETED"

/*
 * Every field is nullable on purpose: Gson fills these from whatever file the user picks, and
 * it happily leaves a missing key null even where Kotlin says it can't be. Reading them as
 * nullable makes each gap explicit — optional fields fall back to their defaults (older
 * backups lack newer fields), required ones refuse the whole file (see validate()).
 */

internal data class BackupSnapshotDto(
    val version: Int? = BACKUP_FORMAT_VERSION,
    val exportedAt: Long? = System.currentTimeMillis(),
    val customExercises: List<ExerciseDto>? = emptyList(),
    val routines: List<RoutineDto>? = emptyList(),
    val sessions: List<SessionDto>? = emptyList(),
    val bodyWeightEntries: List<BodyWeightDto>? = emptyList()
)

/** Exercises are cross-referenced by [name], not row id, so a snapshot is portable across installs. */
internal data class ExerciseDto(
    val name: String?,
    val description: String? = "",
    val instructions: String? = "",
    val muscleGroup: String? = "",
    val exerciseType: String? = "",
    val equipment: String? = "",
    /** Raw Gson-encoded fractional muscle weights, carried through as-is — see Exercise.muscleWeights. */
    val muscleWeightsJson: String? = ""
)

internal data class RoutineDto(
    val name: String?,
    val description: String? = "",
    val type: String? = "GYM",
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = System.currentTimeMillis(),
    val exercises: List<RoutineExerciseDto>? = emptyList()
)

internal data class RoutineExerciseDto(
    val exerciseName: String?,
    val orderIndex: Int? = 0,
    val targetSets: Int? = 3,
    val targetReps: Int? = 10,
    val targetWeight: Float? = 0f,
    val targetDurationSecs: Int? = 0,
    val restBetweenSetsSecs: Int? = 60,
    val notes: String? = "",
    val progressionPolicy: String? = "LINEAR",
    val repRangeLow: Int? = 8,
    val repRangeHigh: Int? = 10,
    val stallThreshold: Int? = 3,
    /** Preserved verbatim — still valid, since exercises re-import in the same order. */
    val supersetGroupId: String? = null,
    val weightUnit: String? = null
)

/** routineId / [endTime] intentionally NOT linked back to a routine — see BackupRepository doc. */
internal data class SessionDto(
    val sessionName: String? = "",
    val startTime: Long?,
    val endTime: Long? = null,
    val totalDurationSecs: Int? = 0,
    val status: String? = STATUS_COMPLETED,
    val notes: String? = "",
    val type: String? = "GYM",
    val sets: List<SetDto>? = emptyList()
)

internal data class SetDto(
    val exerciseName: String?,
    val setNumber: Int? = 1,
    val reps: Int? = null,
    val weight: Float? = null,
    val durationSecs: Int? = null,
    val distance: Float? = null,
    val isCompleted: Boolean? = true,
    val completedAt: Long? = null,
    val notes: String? = "",
    val isWarmup: Boolean? = false,
    val effortType: String? = null,
    val effortValue: Float? = null,
    val weightUnit: String? = null
)

internal data class BodyWeightDto(val weightKg: Float?, val loggedAt: Long?)

internal fun invalid(reason: String): Nothing = throw IllegalArgumentException("Archivo de respaldo inválido: $reason")

private inline fun <reified E : Enum<E>> String?.requireIn(field: String) {
    if (this == null || enumValues<E>().none { it.name == this }) invalid("$field desconocido: $this")
}

/**
 * Checks the whole snapshot before anything is written: a value the app can't read back (an
 * unknown muscle group or workout status, a routine without a name…) would otherwise be stored
 * and then crash every screen that lists it.
 */
internal fun BackupSnapshotDto.validate() {
    if ((version ?: 0) > BACKUP_FORMAT_VERSION) invalid("hecho con una versión más nueva de la app")
    customExercises.orEmpty().forEach { it.validate() }
    routines.orEmpty().forEach { it.validate() }
    sessions.orEmpty().forEach { it.validate() }
    bodyWeightEntries.orEmpty().forEach { it.validate() }
}

private fun ExerciseDto.validate() {
    if (name.isNullOrBlank()) invalid("ejercicio sin nombre")
    muscleGroup.requireIn<MuscleGroup>("grupo muscular")
    exerciseType.requireIn<ExerciseType>("tipo de ejercicio")
}

private fun RoutineDto.validate() {
    if (name == null) invalid("rutina sin nombre")
    (type ?: "GYM").requireIn<WorkoutType>("tipo de rutina")
    exercises.orEmpty().forEach {
        if (it.exerciseName == null) invalid("ejercicio de rutina sin nombre")
        (it.progressionPolicy ?: "LINEAR").requireIn<ProgressionPolicy>("progresión")
    }
}

private fun SessionDto.validate() {
    if (startTime == null) invalid("entrenamiento sin fecha")
    (status ?: STATUS_COMPLETED).requireIn<WorkoutStatus>("estado de entrenamiento")
    (type ?: "GYM").requireIn<WorkoutType>("tipo de entrenamiento")
    sets.orEmpty().forEach { if (it.exerciseName == null) invalid("serie sin ejercicio") }
}

private fun BodyWeightDto.validate() {
    val validWeight = weightKg?.takeIf { kg -> kg.isFinite() && kg > 0f }
    if (validWeight == null || loggedAt == null) invalid("peso corporal inválido")
}
