package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.estimatedOneRepMax
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.model.strengthRetention
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

private const val DAY_MILLIS = 86_400_000f

/**
 * A set counts as "hard" once it's at or above 70% of the exercise's estimated 1RM — a
 * common threshold for "heavy" work in strength training (roughly a 12-rep-max or fewer).
 * Sets below this still count as training, just not for [strengthRetention]'s purposes.
 */
private const val HARD_SET_INTENSITY_THRESHOLD = 0.7f

/**
 * Per muscle group, [strengthRetention] based on days since its last hard (≥70% of estimated
 * 1RM) set — see that function's doc for why this reads differently than acute fatigue or raw
 * weekly volume.
 */
class GetMuscleGroupStrengthRetentionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(): Flow<Map<MuscleGroup, Float>> =
        combine(
            workoutRepository.getAllCompletedSets(),
            exerciseRepository.getAllExercises(),
        ) { sets, exercises ->
            val exerciseById = exercises.associateBy { it.id }
            val weightedSets = sets.excludeWarmups().filter { it.completedAt != null }

            val oneRepMaxByExercise = weightedSets
                .groupBy { it.exerciseId }
                .mapValues { (_, exerciseSets) ->
                    exerciseSets.mapNotNull { estimatedOneRepMax(it.weight ?: 0f, it.reps ?: 0) }.maxOrNull()
                }

            // A hard compound set stresses every muscle it credits (see Exercise.muscleWeights),
            // not just the exercise's primary — a heavy squat is a hard event for hamstrings
            // and glutes too, even though they get less volume credit than quads.
            val lastHardSetByMuscle = mutableMapOf<MuscleGroup, Long>()
            for (set in weightedSets) {
                val exercise = exerciseById[set.exerciseId] ?: continue
                val completedAt = set.completedAt ?: continue
                val oneRepMax = oneRepMaxByExercise[set.exerciseId] ?: continue
                val weight = set.weight ?: 0f
                if (weight / oneRepMax < HARD_SET_INTENSITY_THRESHOLD) continue
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
        }.distinctUntilChanged()
}
