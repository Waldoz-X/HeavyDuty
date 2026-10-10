package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.model.muscleCredits
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * Fractional set count per muscle group in a date range — a compound exercise's set counts as
 * a fraction of a set for each muscle it credits (e.g. 0.6 for the primary, split across
 * secondaries) instead of one whole set for its primary muscle alone, per [muscleCredits].
 */
class GetMuscleGroupSetCountUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<Map<MuscleGroup, Float>> = combine(
        workoutRepository.getCompletedSetsByDateRange(startTime, endTime),
        exerciseRepository.getAllExercises()
    ) { sets, exercises ->
        val exerciseById = exercises.associateBy { it.id }
        val countByMuscle = mutableMapOf<MuscleGroup, Float>()
        sets.excludeWarmups().forEach { set ->
            val exercise = exerciseById[set.exerciseId] ?: return@forEach
            exercise.muscleCredits(1f).forEach { (muscle, credit) ->
                countByMuscle[muscle] = (countByMuscle[muscle] ?: 0f) + credit
            }
        }
        countByMuscle
    }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
