package com.example.heavyduty.feature.exercises.domain.usecase

import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to retrieve all exercises, optionally filtered by muscle group.
 */
class GetExercisesUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(muscleGroup: MuscleGroup? = null): Flow<List<Exercise>> {
        return if (muscleGroup != null) {
            exerciseRepository.getExercisesByMuscleGroup(muscleGroup)
        } else {
            exerciseRepository.getAllExercises()
        }
    }
}
