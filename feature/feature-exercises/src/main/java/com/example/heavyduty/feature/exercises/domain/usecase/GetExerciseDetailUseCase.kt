package com.example.heavyduty.feature.exercises.domain.usecase

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve a single exercise by its ID.
 */
class GetExerciseDetailUseCase @Inject constructor(private val exerciseRepository: ExerciseRepository) {
    operator fun invoke(exerciseId: Long): Flow<Exercise?> = exerciseRepository.getExerciseById(exerciseId)
}
