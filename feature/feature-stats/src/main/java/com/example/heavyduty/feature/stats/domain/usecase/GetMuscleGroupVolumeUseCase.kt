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
 * Aggregates training volume (reps * weight) by muscle group for a date range.
 * Unlike the dashboard's weekly-volume use case, this returns every trained
 * muscle group with no cap, since callers here need the full set for the body
 * diagram and the current-vs-previous comparison radar.
 */
class GetMuscleGroupVolumeUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<Map<MuscleGroup, Float>> = combine(
        workoutRepository.getCompletedSetsByDateRange(startTime, endTime),
        exerciseRepository.getAllExercises()
    ) { sets, exercises ->
        val exerciseById = exercises.associateBy { it.id }
        val volumeByMuscle = mutableMapOf<MuscleGroup, Float>()
        sets.excludeWarmups().forEach { set ->
            val exercise = exerciseById[set.exerciseId] ?: return@forEach
            val setVolume = (set.reps ?: 0) * (set.weight ?: 0f)
            exercise.muscleCredits(setVolume).forEach { (muscle, credit) ->
                volumeByMuscle[muscle] = (volumeByMuscle[muscle] ?: 0f) + credit
            }
        }
        volumeByMuscle
    }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
