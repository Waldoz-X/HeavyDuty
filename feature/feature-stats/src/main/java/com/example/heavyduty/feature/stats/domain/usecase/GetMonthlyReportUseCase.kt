package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.model.muscleCredits
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.stats.domain.model.MonthlyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject

/**
 * Aggregates workouts, sets, volume and the most-trained muscle group for the
 * calendar month containing `referenceMillis` (defaults to the current month).
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val clock: Clock,
) {
    operator fun invoke(referenceMillis: Long = clock.nowMillis()): Flow<MonthlyReport> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = calendar.timeInMillis
        val maxWeek = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH)
        // The month's last millisecond — "23:59:59" alone would drop its final second.
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis - 1

        // Mes anterior para métricas comparativas
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prevMonthStart = prevCal.timeInMillis
        val prevMonthEnd = monthStart - 1

        return combine(
            workoutRepository.getSessionsByDateRange(monthStart, monthEnd),
            workoutRepository.getCompletedSetsByDateRange(monthStart, monthEnd),
            workoutRepository.getSessionsByDateRange(prevMonthStart, prevMonthEnd),
            workoutRepository.getCompletedSetsByDateRange(prevMonthStart, prevMonthEnd),
            exerciseRepository.getAllExercises(),
        ) { sessions, rawSets, prevSessions, prevRawSets, exercises ->
            val exerciseById = exercises.associateBy { it.id }

            // Datos mes actual
            val sets = rawSets.excludeWarmups()
            val completedSessions = sessions.filter { it.status == WorkoutStatus.COMPLETED }
            val sessionsById = completedSessions.associateBy { it.id }
            val completedSets = sets.filter { it.sessionId in sessionsById }

            // Datos mes previo
            val prevCompletedSessions = prevSessions.filter { it.status == WorkoutStatus.COMPLETED }
            val prevSessionsById = prevCompletedSessions.associateBy { it.id }
            val prevCompletedSets = prevRawSets.excludeWarmups().filter { it.sessionId in prevSessionsById }
            val prevTotalVolumeKg = prevCompletedSets.sumOf {
                ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble()
            }.toFloat()

            val volumeByMuscle = mutableMapOf<MuscleGroup, Float>()
            val workoutsPerWeekMap = mutableMapOf<Int, Int>()
            val volumePerWeekMap = mutableMapOf<Int, Float>()

            var totalVolumeKg = 0f
            val cal = Calendar.getInstance()

            completedSessions.forEach { session ->
                cal.timeInMillis = session.startTime
                val week = cal.get(Calendar.WEEK_OF_MONTH)
                workoutsPerWeekMap[week] = (workoutsPerWeekMap[week] ?: 0) + 1
            }

            completedSets.forEach { set ->
                val exercise = exerciseById[set.exerciseId] ?: return@forEach
                val setVolume = (set.reps ?: 0) * (set.weight ?: 0f)
                totalVolumeKg += setVolume
                exercise.muscleCredits(setVolume).forEach { (muscle, credit) ->
                    volumeByMuscle[muscle] = (volumeByMuscle[muscle] ?: 0f) + credit
                }

                val session = sessionsById[set.sessionId]
                if (session != null) {
                    cal.timeInMillis = session.startTime
                    val week = cal.get(Calendar.WEEK_OF_MONTH)
                    volumePerWeekMap[week] = (volumePerWeekMap[week] ?: 0f) + setVolume
                }
            }

            val workoutsPerWeek = (1..maxWeek).map { week ->
                week to (workoutsPerWeekMap[week] ?: 0)
            }
            val volumePerWeek = (1..maxWeek).map { week ->
                week to (volumePerWeekMap[week] ?: 0f)
            }

            // Desglose de contribución por músculo
            val muscleContributions = if (totalVolumeKg > 0f) {
                volumeByMuscle.entries
                    .filter { it.value > 0f }
                    .sortedByDescending { it.value }
                    .map {
                        com.example.heavyduty.feature.stats.domain.model.MuscleVolumeContribution(
                            muscleGroup = it.key,
                            volumeKg = it.value,
                            percentage = (it.value / totalVolumeKg * 100f).coerceIn(0f, 100f),
                        )
                    }
            } else emptyList()

            // Top ejercicios del mes
            val topExercises = completedSets
                .groupBy { it.exerciseId }
                .mapNotNull { (exerciseId, exSets) ->
                    val exercise = exerciseById[exerciseId] ?: return@mapNotNull null
                    val exVolume = exSets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }.toFloat()
                    val maxWeight = exSets.maxOfOrNull { it.weight ?: 0f } ?: 0f
                    com.example.heavyduty.feature.stats.domain.model.TopExerciseMonthly(
                        exerciseId = exerciseId,
                        exerciseName = exercise.displayName,
                        muscleGroup = exercise.muscleGroup,
                        totalSets = exSets.size,
                        totalVolumeKg = exVolume,
                        maxWeightKg = maxWeight,
                    )
                }
                .sortedByDescending { it.totalVolumeKg }
                .take(5)

            // Comparativas vs mes anterior
            val workoutsDelta = if (prevCompletedSessions.isNotEmpty() || completedSessions.isNotEmpty()) {
                completedSessions.size - prevCompletedSessions.size
            } else null

            val volumeDeltaPercent = if (prevTotalVolumeKg > 0f) {
                ((totalVolumeKg - prevTotalVolumeKg) / prevTotalVolumeKg * 100f)
            } else null

            val totalDurationSecs = completedSessions.sumOf { it.totalDurationSecs }
            val avgDurationMins = if (completedSessions.isNotEmpty()) {
                (totalDurationSecs / completedSessions.size / 60)
            } else 0
            val totalDurationMins = (totalDurationSecs / 60)
            val avgPerWeek = if (maxWeek > 0) completedSessions.size.toFloat() / maxWeek.toFloat() else 0f

            MonthlyReport(
                monthStartMillis = monthStart,
                referenceMillis = referenceMillis,
                totalWorkouts = completedSessions.size,
                totalSets = completedSets.size,
                totalVolumeKg = totalVolumeKg,
                totalDurationMinutes = totalDurationMins,
                avgSessionDurationMinutes = avgDurationMins,
                workoutsPerWeekAverage = avgPerWeek,
                prevMonthWorkoutsDelta = workoutsDelta,
                prevMonthVolumeDeltaPercent = volumeDeltaPercent,
                prevMonthVolumeKg = prevTotalVolumeKg,
                topMuscleGroup = volumeByMuscle.maxByOrNull { it.value }?.key,
                muscleContributions = muscleContributions,
                topExercises = topExercises,
                workoutsPerWeek = workoutsPerWeek,
                volumePerWeek = volumePerWeek,
            )
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
    }
}
