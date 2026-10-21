package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.creditByMuscle
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.feature.history.domain.model.ExerciseSets
import com.example.heavyduty.feature.history.domain.model.ShareRecord
import com.example.heavyduty.feature.history.domain.model.WorkoutShareSummary
import com.example.heavyduty.feature.history.domain.model.exerciseSets
import com.example.heavyduty.feature.history.domain.model.heaviestThenMostReps
import com.example.heavyduty.feature.history.domain.model.shareExerciseLines
import com.example.heavyduty.feature.history.domain.model.workingSetCount
import com.example.heavyduty.feature.history.domain.model.workingVolumeKg
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * What a workout's share card says: its numbers, the muscles it trained and the records it set.
 *
 * A record is a set heavier than any in the finished workouts that started before this one (or as
 * heavy, with more reps) — so an old workout keeps the records it set at the time. The first time
 * an exercise is ever done is a starting point, not a record.
 */
class BuildWorkoutShareSummaryUseCase @Inject constructor(private val trainingHistoryRepository: TrainingHistoryRepository) {
    suspend operator fun invoke(session: WorkoutSession): WorkoutShareSummary {
        val exercises = session.exerciseSets()
        val exerciseById = session.sets.mapNotNull { it.exercise }.associateBy { it.id }
        return WorkoutShareSummary(
            startTime = session.startTime,
            durationSecs = session.totalDurationSecs,
            workingSets = session.workingSetCount,
            volumeKg = session.workingVolumeKg,
            exercises = shareExerciseLines(exercises),
            records = exercises.mapNotNull { newRecord(it, session.startTime) },
            setsByMuscle = session.sets.excludeWarmups().creditByMuscle(exerciseById) { 1f }
        )
    }

    private suspend fun newRecord(exercise: ExerciseSets, startTime: Long): ShareRecord? {
        val best = exercise.sets.liftedWorkingSets().maxWithOrNull(heaviestThenMostReps) ?: return null
        val previousBest = trainingHistoryRepository.getCompletedSetsByExerciseIdBefore(exercise.exerciseId, startTime)
            .first()
            .liftedWorkingSets()
            .maxWithOrNull(heaviestThenMostReps)
            ?: return null
        return if (heaviestThenMostReps.compare(best, previousBest) > 0) ShareRecord(exercise.name, best) else null
    }

    /** Sets a strength record can come from: working sets with a weight and reps. */
    private fun List<WorkoutSet>.liftedWorkingSets() = excludeWarmups().filter { (it.weight ?: 0f) > 0f && (it.reps ?: 0) > 0 }
}
