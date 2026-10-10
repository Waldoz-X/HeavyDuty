package com.example.heavyduty.feature.stats.domain.model

import com.example.heavyduty.core.domain.model.MuscleGroup

data class MuscleVolumeContribution(
    val muscleGroup: MuscleGroup,
    val volumeKg: Float,
    // 0.0 to 100.0
    val percentage: Float
)

data class TopExerciseMonthly(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val totalSets: Int,
    val totalVolumeKg: Float,
    val maxWeightKg: Float
)

/**
 * Aggregated training summary for a single calendar month.
 */
data class MonthlyReport(
    /** First instant of the reported month; the UI formats its name in the app's language. */
    val monthStartMillis: Long = 0L,
    val referenceMillis: Long = 0L,
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalVolumeKg: Float = 0f,
    val totalDurationMinutes: Int = 0,
    val avgSessionDurationMinutes: Int = 0,
    val workoutsPerWeekAverage: Float = 0f,

    // Comparativas vs mes anterior
    val prevMonthWorkoutsDelta: Int? = null,
    val prevMonthVolumeDeltaPercent: Float? = null,
    val prevMonthVolumeKg: Float? = null,

    // Desglose muscular
    val topMuscleGroup: MuscleGroup? = null,
    val muscleContributions: List<MuscleVolumeContribution> = emptyList(),

    // Top ejercicios del mes
    val topExercises: List<TopExerciseMonthly> = emptyList(),

    // Desglose semanal, por número de semana del mes (1, 2, …); la UI pone la etiqueta.
    val workoutsPerWeek: List<Pair<Int, Int>> = emptyList(),
    val volumePerWeek: List<Pair<Int, Float>> = emptyList()
)
