package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.feature.history.domain.model.ExercisePersonalRecords
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case that derives personal records (heaviest set, most reps, estimated 1RM) for a
 * single exercise from its logged set history.
 */
class GetExercisePersonalRecordsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<ExercisePersonalRecords> =
        workoutRepository.getSetsByExerciseId(exerciseId).map { sets -> computeRecords(sets) }

    private fun computeRecords(sets: List<WorkoutSet>): ExercisePersonalRecords {
        val weightedSets = sets.excludeWarmups().filter { (it.weight ?: 0f) > 0f && (it.reps ?: 0) > 0 }
        if (weightedSets.isEmpty()) return ExercisePersonalRecords()

        val maxWeightSet = weightedSets.maxBy { it.weight ?: 0f }
        val maxRepsSet = weightedSets.maxBy { it.reps ?: 0 }
        val bestEstimatedOneRepMax = weightedSets.maxOf { estimateOneRepMax(it.weight ?: 0f, it.reps ?: 0) }

        return ExercisePersonalRecords(
            maxWeight = maxWeightSet.weight,
            maxWeightReps = maxWeightSet.reps,
            maxWeightAchievedAtMillis = maxWeightSet.completedAt,
            maxReps = maxRepsSet.reps,
            maxRepsWeight = maxRepsSet.weight,
            estimatedOneRepMax = bestEstimatedOneRepMax,
        )
    }

    /** Epley formula: a standard, widely-used estimate — not a substitute for an actual 1RM test. */
    private fun estimateOneRepMax(weight: Float, reps: Int): Float =
        weight * (1f + reps / 30f)
}
