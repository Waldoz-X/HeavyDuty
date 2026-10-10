package com.example.heavyduty.feature.routines.domain.usecase

import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.repository.RoutineRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve a routine with all its exercises.
 */
class GetRoutineWithExercisesUseCase @Inject constructor(private val routineRepository: RoutineRepository) {
    operator fun invoke(routineId: Long): Flow<Routine?> = routineRepository.getRoutineWithExercises(routineId)
}
