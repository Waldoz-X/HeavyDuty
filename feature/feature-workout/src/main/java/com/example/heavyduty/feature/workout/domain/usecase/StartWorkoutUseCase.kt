package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject

/**
 * Use case to start a new workout session.
 */
class StartWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        routineId: Long? = null,
        sessionName: String = "",
        type: WorkoutType = WorkoutType.GYM,
        startTime: Long = clock.nowMillis(),
    ): Long {
        val session = WorkoutSession(
            routineId = routineId,
            sessionName = sessionName,
            startTime = startTime,
            status = WorkoutStatus.IN_PROGRESS,
            type = type,
        )
        return workoutRepository.insertSession(session)
    }
}
