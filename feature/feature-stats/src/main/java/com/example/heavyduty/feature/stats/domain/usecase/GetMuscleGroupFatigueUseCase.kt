package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.stats.domain.model.FatigueState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/**
 * Classifies every muscle group by how recently it was last trained (see [FatigueState]).
 */
class GetMuscleGroupFatigueUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<Map<MuscleGroup, FatigueState>> =
        combine(
            workoutRepository.getAllCompletedSets(),
            exerciseRepository.getAllExercises(),
        ) { sets, exercises ->
            val exerciseById = exercises.associateBy { it.id }
            val lastTrainedByMuscle = mutableMapOf<MuscleGroup, Long>()
            sets.excludeWarmups().forEach { set ->
                val completedAt = set.completedAt ?: return@forEach
                val exercise = exerciseById[set.exerciseId] ?: return@forEach
                val current = lastTrainedByMuscle[exercise.muscleGroup]
                if (current == null || completedAt > current) {
                    lastTrainedByMuscle[exercise.muscleGroup] = completedAt
                }
            }
            val now = System.currentTimeMillis()
            MuscleGroup.entries.associateWith { muscle ->
                val lastTrained = lastTrainedByMuscle[muscle]
                if (lastTrained == null) {
                    FatigueState.READY
                } else {
                    val daysSince = (now - lastTrained) / DAY_MILLIS
                    when {
                        daysSince <= 1 -> FatigueState.FATIGUED
                        daysSince <= 3 -> FatigueState.RECOVERING
                        else -> FatigueState.READY
                    }
                }
            }
        }.distinctUntilChanged()
}
