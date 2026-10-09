package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.HARD_SET_INTENSITY
import com.example.heavyduty.core.domain.model.strengthRetention
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

private const val DAY_MILLIS = 86_400_000f

/**
 * Per muscle group, [strengthRetention] based on days since its last hard set (≥ [HARD_SET_INTENSITY]
 * of estimated 1RM) — see that function's doc for why this reads differently than acute fatigue or raw
 * weekly volume.
 */
class GetMuscleGroupStrengthRetentionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<Map<MuscleGroup, Float>> =
        combine(
            workoutRepository.getLastHardSetTimes(),
            exerciseRepository.getAllExercises(),
        ) { lastHardSetByExercise, exercises ->
            // A hard compound set stresses every muscle it credits (see Exercise.muscleWeights),
            // not just the exercise's primary — a heavy squat is a hard event for hamstrings
            // and glutes too, even though they get less volume credit than quads.
            val lastHardSetByMuscle = mutableMapOf<MuscleGroup, Long>()
            for (exercise in exercises) {
                val completedAt = lastHardSetByExercise[exercise.id] ?: continue
                exercise.muscleWeights.keys.forEach { muscle ->
                    val current = lastHardSetByMuscle[muscle]
                    if (current == null || completedAt > current) {
                        lastHardSetByMuscle[muscle] = completedAt
                    }
                }
            }

            val now = System.currentTimeMillis()
            MuscleGroup.entries.associateWith { muscle ->
                val daysSince = lastHardSetByMuscle[muscle]?.let { (now - it) / DAY_MILLIS }
                strengthRetention(daysSince)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
