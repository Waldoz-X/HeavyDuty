package com.example.heavyduty.feature.routines.domain.usecase

import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.repository.RoutineRepository
import javax.inject.Inject

/**
 * Use case to update an existing routine.
 */
class UpdateRoutineUseCase @Inject constructor(private val routineRepository: RoutineRepository) {
    suspend operator fun invoke(routine: Routine) = routineRepository.updateRoutine(routine)
}
