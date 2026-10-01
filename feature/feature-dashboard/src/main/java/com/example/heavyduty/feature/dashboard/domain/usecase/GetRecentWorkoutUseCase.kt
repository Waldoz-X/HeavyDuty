package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The most recently finished workout, or null if none has been completed yet. */
class GetRecentWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(): Flow<WorkoutSession?> =
        workoutRepository.getAllSessions()
            .map { sessions ->
                sessions
                    .filter { it.status == WorkoutStatus.COMPLETED && it.endTime != null }
                    .maxByOrNull { it.endTime!! }
            }
            .distinctUntilChanged()
}
