package com.example.heavyduty.core.domain.model

import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow

private const val FATIGUE_HALF_LIFE_HOURS = 36.0
private const val INTENSITY_EXPONENT = 1.5

/**
 * A completed set's contribution to muscle fatigue/stimulus, weighted by how close it was to
 * the estimated 1RM: weight × reps × min(1, weight/1RM)^1.5 — a set at 90% of estimated 1RM
 * counts ~0.81× its raw tonnage (weight × reps); at 50%, only ~0.35×. Raw tonnage alone treats
 * a warmup and a near-max effort as equally fatiguing, which isn't honest about what actually
 * happened.
 *
 * Falls back to raw tonnage when there's no 1RM estimate yet for the exercise (e.g. its only
 * logged set is over [estimatedOneRepMax]'s 12-rep cap) — better to count it at face value
 * than to silently drop it from the fatigue picture.
 */
fun intensityWeightedTonnage(weightKg: Float, reps: Int, estimatedOneRepMaxKg: Float?): Float {
    if (weightKg <= 0f || reps <= 0) return 0f
    val rawTonnage = weightKg * reps
    val oneRepMax = estimatedOneRepMaxKg?.takeIf { it > 0f }
    val intensityFactor = if (oneRepMax == null) 1f else min(1f, weightKg / oneRepMax).toDouble().pow(INTENSITY_EXPONENT).toFloat()
    return rawTonnage * intensityFactor
}

/**
 * Exponentially decays [previousValue] over [hoursElapsed] (36h half-life) and folds in
 * [newStimulus] — more volume raises the level, but the fold-in is additive rather than a hard
 * reset, and the decay is continuous instead of dropping all at once at midnight. Nothing is
 * persisted between calls: callers fold this over a muscle's full chronological set history,
 * same as [nextPrescription] recomputes from history instead of storing running state.
 *
 * The result is an unbounded accumulator, not a 0..1 reading — pass it through [fatigueLevel]
 * for that.
 */
fun decayedFatigue(previousValue: Float, newStimulus: Float, hoursElapsed: Float): Float {
    val decay = 0.5.pow(hoursElapsed / FATIGUE_HALF_LIFE_HOURS).toFloat()
    return previousValue * decay + newStimulus
}

/**
 * Maps [decayedFatigue]'s unbounded accumulator to a 0 (fresh) .. 1 (maximally fatigued)
 * reading via 1 − e^(−value), so more volume keeps pushing the level up without ever pinning
 * it exactly at 1.
 */
fun fatigueLevel(accumulatedValue: Float): Float = (1.0 - exp(-accumulatedValue.toDouble())).toFloat()
