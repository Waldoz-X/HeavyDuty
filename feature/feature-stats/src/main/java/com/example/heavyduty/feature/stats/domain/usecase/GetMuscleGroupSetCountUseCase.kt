package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Counts how many sets were logged per muscle group in a date range.
 */
class GetMuscleGroupSetCountUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<Map<MuscleGroup, Int>> =
        combine(
            workoutRepository.getCompletedSetsByDateRange(startTime, endTime),
            exerciseRepository.getAllExercises(),
        ) { sets, exercises ->
            val exerciseById = exercises.associateBy { it.id }
            val countByMuscle = mutableMapOf<MuscleGroup, Int>()
            sets.forEach { set ->
                val exercise = exerciseById[set.exerciseId] ?: return@forEach
                countByMuscle[exercise.muscleGroup] = (countByMuscle[exercise.muscleGroup] ?: 0) + 1
            }
            countByMuscle
        }.distinctUntilChanged()
}
