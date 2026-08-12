package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject

/**
 * Use case to log a single set during an active workout session.
 */
class LogSetUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        exerciseId: Long,
        setNumber: Int,
        reps: Int? = null,
        weight: Float? = null,
        durationSecs: Int? = null,
        distance: Float? = null,
    ): Long {
        val set = WorkoutSet(
            sessionId = sessionId,
            exerciseId = exerciseId,
            setNumber = setNumber,
            reps = reps,
            weight = weight,
            durationSecs = durationSecs,
            distance = distance,
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
        )
        return workoutRepository.insertSet(set)
    }
}
