package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.countedFor
import com.example.heavyduty.core.domain.model.muscleCredits
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Aggregates training volume (reps * weight) by muscle group for a date range,
 * keeping only the top axes so the radar chart stays readable. Counts the same sets as the
 * weekly volume total (see [countedFor]), so a workout still in progress doesn't show up yet.
 */
class GetWeeklyMuscleVolumeUseCase @Inject constructor(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<Map<MuscleGroup, Float>> = combine(
        trainingHistoryRepository.getSessionsByDateRange(startTime, endTime),
        trainingHistoryRepository.getCompletedSetsByDateRange(startTime, endTime),
        exerciseRepository.getAllExercises()
    ) { sessions, sets, exercises ->
        val exerciseById = exercises.associateBy { it.id }
        val volumeByMuscle = mutableMapOf<MuscleGroup, Float>()
        sets.countedFor(sessions).forEach { set ->
            val exercise = exerciseById[set.exerciseId] ?: return@forEach
            val setVolume = (set.reps ?: 0) * (set.weight ?: 0f)
            exercise.muscleCredits(setVolume).forEach { (muscle, credit) ->
                volumeByMuscle[muscle] = (volumeByMuscle[muscle] ?: 0f) + credit
            }
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
