package com.example.heavyduty.feature.history.domain.usecase

import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.estimatedOneRepMax
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.feature.history.domain.model.ExercisePersonalRecords
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that derives personal records (heaviest set, most reps, estimated 1RM) for a
 * single exercise from its logged set history.
 */
class GetExercisePersonalRecordsUseCase @Inject constructor(private val trainingHistoryRepository: TrainingHistoryRepository) {
    operator fun invoke(exerciseId: Long): Flow<ExercisePersonalRecords> =
        trainingHistoryRepository.getSetsByExerciseId(exerciseId).map { sets -> computeRecords(sets) }

    private fun computeRecords(sets: List<WorkoutSet>): ExercisePersonalRecords {
        val weightedSets = sets.excludeWarmups().filter { (it.weight ?: 0f) > 0f && (it.reps ?: 0) > 0 }
        if (weightedSets.isEmpty()) return ExercisePersonalRecords()

        val maxWeightSet = weightedSets.maxBy { it.weight ?: 0f }
        val maxRepsSet = weightedSets.maxBy { it.reps ?: 0 }
        val bestEstimatedOneRepMax = weightedSets
            .mapNotNull { estimatedOneRepMax(it.weight ?: 0f, it.reps ?: 0) }
            .maxOrNull()

        return ExercisePersonalRecords(
            maxWeight = maxWeightSet.weight,
            maxWeightReps = maxWeightSet.reps,
            maxWeightAchievedAtMillis = maxWeightSet.completedAt,
            maxReps = maxRepsSet.reps,
            maxRepsWeight = maxRepsSet.weight,
            estimatedOneRepMax = bestEstimatedOneRepMax
        )
    }
}
