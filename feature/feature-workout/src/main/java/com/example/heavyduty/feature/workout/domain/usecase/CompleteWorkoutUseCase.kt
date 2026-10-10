package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.workoutEndTime
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Finishes the workout in progress. It ends now — or, when it sat open long after its last set,
 * at that set (see [workoutEndTime]), so a workout forgotten overnight doesn't count as 35 hours.
 */
class CompleteWorkoutUseCase @Inject constructor(private val workoutRepository: WorkoutRepository, private val clock: Clock) {
    suspend operator fun invoke(sessionId: Long) {
        val session = workoutRepository.getSessionWithSets(sessionId).firstOrNull() ?: return
        val endTime = workoutEndTime(session.startTime, session.sets, now = clock.nowMillis())
        val totalDuration = ((endTime - session.startTime) / 1000).toInt()

        workoutRepository.updateSession(
            session.copy(
                endTime = endTime,
                totalDurationSecs = totalDuration,
                status = WorkoutStatus.COMPLETED
            )
        )
    }
}
