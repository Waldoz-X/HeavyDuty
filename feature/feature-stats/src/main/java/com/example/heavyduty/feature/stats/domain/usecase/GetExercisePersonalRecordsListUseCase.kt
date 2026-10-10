package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.estimatedOneRepMax
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.stats.domain.model.ExerciseRanking
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * The best estimated one-rep max ([estimatedOneRepMax]) reached on each exercise — used as
 * a local stand-in for a "ranking eligible" exercise list, since this app has no social
 * backend to compare against other users.
 */
class GetExercisePersonalRecordsListUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(): Flow<List<ExerciseRanking>> = combine(
        workoutRepository.getExerciseSetSummaries(),
        exerciseRepository.getAllExercises()
    ) { summaries, exercises ->
        val exerciseById = exercises.associateBy { it.id }
        summaries
            .mapNotNull { summary ->
                val exercise = exerciseById[summary.exerciseId] ?: return@mapNotNull null
                val oneRepMax = summary.bestOneRepMaxKg ?: return@mapNotNull null
                ExerciseRanking(
                    exerciseId = summary.exerciseId,
                    exerciseName = exercise.displayName,
                    estimatedOneRepMaxKg = oneRepMax,
                    sourceWeightKg = summary.bestSetWeightKg ?: 0f,
                    sourceReps = summary.bestSetReps ?: 0,
                    achievedAtMillis = summary.bestSetAt
                )
            }
            .sortedByDescending { it.estimatedOneRepMaxKg }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
