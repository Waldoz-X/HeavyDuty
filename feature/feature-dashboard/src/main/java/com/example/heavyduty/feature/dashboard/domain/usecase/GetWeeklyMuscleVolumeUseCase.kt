package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Aggregates training volume (reps * weight) by muscle group for a date range,
 * keeping only the top axes so the radar chart stays readable.
 */
class GetWeeklyMuscleVolumeUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<Map<MuscleGroup, Float>> =
        combine(
            workoutRepository.getCompletedSetsByDateRange(startTime, endTime),
            exerciseRepository.getAllExercises(),
        ) { sets, exercises ->
            val exerciseById = exercises.associateBy { it.id }
            val volumeByMuscle = mutableMapOf<MuscleGroup, Float>()
            sets.forEach { set ->
                val exercise = exerciseById[set.exerciseId] ?: return@forEach
                val setVolume = (set.reps ?: 0) * (set.weight ?: 0f)
                volumeByMuscle[exercise.muscleGroup] = (volumeByMuscle[exercise.muscleGroup] ?: 0f) + setVolume
            }
            volumeByMuscle.entries
                .sortedByDescending { it.value }
                .take(MAX_MUSCLE_AXES)
                .associate { it.key to it.value }
        }.distinctUntilChanged()

    private companion object {
        const val MAX_MUSCLE_AXES = 8
    }
}
