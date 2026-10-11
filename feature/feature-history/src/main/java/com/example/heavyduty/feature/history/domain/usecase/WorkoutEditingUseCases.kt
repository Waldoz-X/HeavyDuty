package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/** The longest a workout can be set to last: a day. Anything longer is the mistake being fixed. */
val MAX_WORKOUT_MINUTES: Int = TimeUnit.DAYS.toMinutes(1).toInt()

/** The highest rep count accepted for a set — guards against a typo like "800". */
const val MAX_REPS = 999

/** One finished workout with its sets, each carrying its exercise. */
class GetWorkoutDetailUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    operator fun invoke(sessionId: Long): Flow<WorkoutSession?> = workoutRepository.getSessionWithSets(sessionId)
}

/**
 * Corrects how long a workout lasted — e.g. one left open overnight before the app ended
 * forgotten workouts at their last set. The start stays; the end moves to match.
 */
class UpdateWorkoutDurationUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    /** False (and nothing saved) for a duration outside 1 minute to a day. */
    suspend operator fun invoke(sessionId: Long, minutes: Int): Boolean {
        val session = workoutRepository.getSessionById(sessionId).firstOrNull()
        if (session == null || minutes !in 1..MAX_WORKOUT_MINUTES) return false
        workoutRepository.updateSession(
            session.copy(
                endTime = session.startTime + TimeUnit.MINUTES.toMillis(minutes.toLong()),
                totalDurationSecs = minutes * SECONDS_PER_MINUTE
            )
        )
        return true
    }
}

/** Fixes a logged set's weight (kg) and reps; its time, unit and exercise stay as logged. */
class UpdateLoggedSetUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    /** False (and nothing saved) for a negative weight or reps outside 1..[MAX_REPS]. */
    suspend operator fun invoke(set: WorkoutSet, weightKg: Float, reps: Int): Boolean {
        if (weightKg < 0f || reps !in 1..MAX_REPS) return false
        workoutRepository.updateSet(set.copy(weight = weightKg, reps = reps))
        return true
    }
}

/** Deletes a workout from the history, with all its sets (the database cascades them). */
class DeleteWorkoutUseCase @Inject constructor(private val workoutRepository: WorkoutRepository) {
    suspend operator fun invoke(sessionId: Long) {
        val session = workoutRepository.getSessionById(sessionId).firstOrNull() ?: return
        workoutRepository.deleteSession(session)
    }
}

private const val SECONDS_PER_MINUTE = 60
