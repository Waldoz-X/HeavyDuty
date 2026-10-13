package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.TrainingStatsRepository
import com.example.heavyduty.feature.stats.domain.model.ExerciseFrequency
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * Ranks exercises by how many sets have been logged for them, all-time.
 */
class GetTopExercisesByFrequencyUseCase @Inject constructor(
    private val trainingStatsRepository: TrainingStatsRepository,
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(): Flow<List<ExerciseFrequency>> = combine(
        trainingStatsRepository.getExerciseSetSummaries(),
        exerciseRepository.getAllExercises()
    ) { summaries, exercises ->
        val exerciseById = exercises.associateBy { it.id }
        summaries
            .mapNotNull { summary ->
                val exercise = exerciseById[summary.exerciseId] ?: return@mapNotNull null
                ExerciseFrequency(
                    exerciseId = summary.exerciseId,
                    exerciseName = exercise.displayName,
                    setCount = summary.workingSets,
                    imageUrl = exercise.imageUrl
                )
            }
            .sortedByDescending { it.setCount }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
