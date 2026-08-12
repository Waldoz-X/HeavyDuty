package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case to complete an active workout session.
 * Calculates total duration and updates status.
 */
class CompleteWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: Long) {
        val session = workoutRepository.getSessionById(sessionId).firstOrNull() ?: return
        val endTime = System.currentTimeMillis()
        val totalDuration = ((endTime - session.startTime) / 1000).toInt()

        workoutRepository.updateSession(
            session.copy(
                endTime = endTime,
                totalDurationSecs = totalDuration,
                status = WorkoutStatus.COMPLETED,
            ),
        )
    }
}
