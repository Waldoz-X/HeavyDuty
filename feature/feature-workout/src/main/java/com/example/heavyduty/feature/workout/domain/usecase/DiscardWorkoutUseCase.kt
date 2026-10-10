package com.example.heavyduty.feature.workout.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Throws away a workout started by mistake: the session and every set logged in it (the sets
 * go with it through the database's cascade). Only a workout still in progress — a finished
 * one is history, and this must never be a way to lose it.
 */
class DiscardWorkoutUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    suspend operator fun invoke(sessionId: Long) {
        val session = workoutRepository.getSessionById(sessionId).firstOrNull() ?: return
        if (session.status != WorkoutStatus.IN_PROGRESS) return
        workoutRepository.deleteSession(session)
    }
}
