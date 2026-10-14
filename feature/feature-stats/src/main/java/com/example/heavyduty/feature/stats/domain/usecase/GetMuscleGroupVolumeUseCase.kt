package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.creditByMuscle
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
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
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val exerciseRepository: ExerciseRepository,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<Map<MuscleGroup, Float>> = combine(
        trainingHistoryRepository.getCompletedSetsByDateRange(startTime, endTime),
        exerciseRepository.getAllExercises()
    ) { sets, exercises ->
        sets.excludeWarmups().creditByMuscle(exercises.associateBy { it.id }) { set -> (set.reps ?: 0) * (set.weight ?: 0f) }
    }.distinctUntilChanged().flowOn(defaultDispatcher)
}
