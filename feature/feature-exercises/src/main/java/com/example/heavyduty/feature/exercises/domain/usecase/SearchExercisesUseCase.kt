package com.example.heavyduty.feature.exercises.domain.usecase

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to search exercises by name or description query.
 */
class SearchExercisesUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(query: String): Flow<List<Exercise>> =
        exerciseRepository.searchExercises(query)
}
