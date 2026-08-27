package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.stats.domain.model.ExerciseFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Ranks exercises by how many sets have been logged for them, all-time.
 */
class GetTopExercisesByFrequencyUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<List<ExerciseFrequency>> =
        combine(
            workoutRepository.getAllCompletedSets(),
            exerciseRepository.getAllExercises(),
        ) { sets, exercises ->
            val exerciseById = exercises.associateBy { it.id }
            sets.groupBy { it.exerciseId }
                .mapNotNull { (exerciseId, setsForExercise) ->
                    val exercise = exerciseById[exerciseId] ?: return@mapNotNull null
                    ExerciseFrequency(
                        exerciseId = exerciseId,
                        exerciseName = exercise.name,
                        setCount = setsForExercise.size,
                    )
                }
                .sortedByDescending { it.setCount }
        }.distinctUntilChanged()
}
