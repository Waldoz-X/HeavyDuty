package com.example.heavyduty.feature.routines.domain.usecase

import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve all routines.
 */
class GetRoutinesUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
) {
    operator fun invoke(): Flow<List<Routine>> =
        routineRepository.getAllRoutines()
}
