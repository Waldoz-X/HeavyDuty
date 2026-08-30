package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.decayedFatigue
import com.example.heavyduty.core.domain.model.estimatedOneRepMax
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.model.fatigueLevel
import com.example.heavyduty.core.domain.model.intensityWeightedTonnage
import com.example.heavyduty.core.domain.model.muscleCredits
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Calibration divisor for [intensityWeightedTonnage]'s raw kg-tonnage output before it's
 * folded into [decayedFatigue]'s accumulator — chosen so a single hard working set (e.g.
 * ~80kg × 8 reps at ~85% of estimated 1RM, tonnage ≈500) lands around 1.0..1.5 of stimulus,
 * which reads as "clearly fatigued" (see [fatigueLevel]) without saturating instantly. This
 * is our own calibration, not a value taken from any reference app.
 */
private const val FATIGUE_STIMULUS_DIVISOR = 400f

/**
 * Continuous 0 (fresh) .. 1 (maximally fatigued) reading per muscle group, weighted by how
 * intense each set was relative to the exercise's estimated 1RM and decayed smoothly over
 * time (36h half-life) — replaces a purely recency-based day-bucket estimate, which treated a
 * single warmup set the same as a brutal top set and dropped straight from "fatigued" to
 * "ready" at a fixed day boundary.
 */
class GetMuscleGroupFatigueUseCase @Inject constructor(
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

            // Each set fans its stimulus out across every muscle it credits (see
            // Exercise.muscleCredits), not just its exercise's single primary muscle group.
            val stimulusByMuscle = mutableMapOf<MuscleGroup, MutableList<Pair<Long, Float>>>()
            weightedSets.forEach { set ->
                val exercise = exerciseById[set.exerciseId] ?: return@forEach
                val timestamp = set.completedAt ?: return@forEach
                val rawStimulus = intensityWeightedTonnage(
                    weightKg = set.weight ?: 0f,
                    reps = set.reps ?: 0,
                    estimatedOneRepMaxKg = oneRepMaxByExercise[set.exerciseId],
                ) / FATIGUE_STIMULUS_DIVISOR
                exercise.muscleCredits(rawStimulus).forEach { (muscle, weightedStimulus) ->
                    stimulusByMuscle.getOrPut(muscle) { mutableListOf() }.add(timestamp to weightedStimulus)
                }
            }

            val now = System.currentTimeMillis()
            val fatigueByMuscle = stimulusByMuscle.mapValues { (_, entries) ->
                accumulateFatigue(entries.sortedBy { it.first }, now)
            }

            MuscleGroup.entries.associateWith { fatigueLevel(fatigueByMuscle[it] ?: 0f) }
        }.distinctUntilChanged()

    private fun accumulateFatigue(chronologicalStimulus: List<Pair<Long, Float>>, now: Long): Float {
        var accumulator = 0f
        var lastTimestamp: Long? = null
        for ((timestamp, stimulus) in chronologicalStimulus) {
            val hoursElapsed = lastTimestamp?.let { (timestamp - it) / 3_600_000f } ?: 0f
            accumulator = decayedFatigue(accumulator, stimulus, hoursElapsed)
            lastTimestamp = timestamp
        }
        val hoursSinceLast = lastTimestamp?.let { (now - it) / 3_600_000f } ?: return accumulator
        return decayedFatigue(accumulator, 0f, hoursSinceLast)
    }
}
