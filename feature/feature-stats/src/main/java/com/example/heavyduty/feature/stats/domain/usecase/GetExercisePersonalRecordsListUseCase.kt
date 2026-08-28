package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.stats.domain.model.ExerciseRanking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * The heaviest completed set ever logged for each exercise — used as a local
 * stand-in for a "ranking eligible" exercise list, since this app has no social
 * backend to compare against other users.
 */
class GetExercisePersonalRecordsListUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<List<ExerciseRanking>> =
        combine(
            workoutRepository.getAllCompletedSets(),
            exerciseRepository.getAllExercises(),
        ) { sets, exercises ->
            val exerciseById = exercises.associateBy { it.id }
            sets.excludeWarmups()
                .filter { it.weight != null }
                .groupBy { it.exerciseId }
                .mapNotNull { (exerciseId, setsForExercise) ->
                    val exercise = exerciseById[exerciseId] ?: return@mapNotNull null
                    val bestSet = setsForExercise.maxByOrNull { it.weight ?: 0f } ?: return@mapNotNull null
                    ExerciseRanking(
                        exerciseId = exerciseId,
                        exerciseName = exercise.name,
                        maxWeightKg = bestSet.weight ?: 0f,
                        repsAtMaxWeight = bestSet.reps,
                        achievedAtMillis = bestSet.completedAt,
                    )
                }
                .sortedByDescending { it.maxWeightKg }
        }.distinctUntilChanged()
}
