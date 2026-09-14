package com.example.heavyduty.feature.stats.domain.usecase

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Aggregates workouts, sets, volume and the most-trained muscle group for the
 * calendar month containing [referenceMillis] (defaults to the current month).
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(referenceMillis: Long = System.currentTimeMillis()): Flow<MonthlyReport> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.Builder().setLanguage("es").setRegion("ES").build())
            .format(calendar.time)
        val monthStart = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val monthEnd = calendar.timeInMillis

        return combine(
            workoutRepository.getSessionsByDateRange(monthStart, monthEnd),
            workoutRepository.getCompletedSetsByDateRange(monthStart, monthEnd),
            exerciseRepository.getAllExercises(),
        ) { sessions, rawSets, exercises ->
            val sets = rawSets.excludeWarmups()
            val completedSessions = sessions.filter { it.status == WorkoutStatus.COMPLETED }
            val exerciseById = exercises.associateBy { it.id }
            val volumeByMuscle = mutableMapOf<MuscleGroup, Float>()
            var totalVolumeKg = 0f
            sets.forEach { set ->
                val exercise = exerciseById[set.exerciseId] ?: return@forEach
                val setVolume = (set.reps ?: 0) * (set.weight ?: 0f)
                totalVolumeKg += setVolume
                exercise.muscleCredits(setVolume).forEach { (muscle, credit) ->
                    volumeByMuscle[muscle] = (volumeByMuscle[muscle] ?: 0f) + credit
                }
            }
            val totalDurationSecs = completedSessions.sumOf { it.totalDurationSecs }
            MonthlyReport(
                monthLabel = monthLabel.replaceFirstChar { it.uppercase() },
                totalWorkouts = completedSessions.size,
                totalSets = sets.size,
                totalVolumeKg = totalVolumeKg,
                avgSessionDurationMinutes = if (completedSessions.isNotEmpty()) {
                    totalDurationSecs / completedSessions.size / 60
                } else {
                    0
                },
                topMuscleGroup = volumeByMuscle.maxByOrNull { it.value }?.key,
            )
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
    }
}
