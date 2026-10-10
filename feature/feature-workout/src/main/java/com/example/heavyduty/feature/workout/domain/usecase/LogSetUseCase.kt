package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.RestTimerController
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject

/**
 * Use case to log a single set during an active workout session.
 */
class LogSetUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val restTimerController: RestTimerController,
    private val clock: Clock
) {
    suspend operator fun invoke(
        sessionId: Long,
        exerciseId: Long,
        setNumber: Int,
        reps: Int? = null,
        weight: Float? = null,
        durationSecs: Int? = null,
        distance: Float? = null,
        isWarmup: Boolean = false,
        effort: Effort? = null,
        restSeconds: Int? = null,
        weightUnit: WeightUnit? = null
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
            completedAt = clock.nowMillis(),
            isWarmup = isWarmup,
            effort = effort,
            weightUnit = weightUnit
        )
        val id = workoutRepository.insertSet(set)
        if (restSeconds != null && restSeconds > 0) {
            restTimerController.startRest(restSeconds)
        }
        return id
    }
}
