package com.example.heavyduty.core.domain.model

import kotlin.math.pow

private const val FULL_RETENTION_DAYS = 14.0
private const val RETENTION_HALF_LIFE_DAYS = 28.0
private const val RETENTION_FLOOR = 0.5f

/**
 * How much of a muscle's trained strength is still "in the bank" — distinct from
 * [decayedFatigue]'s acute, fast-decaying fatigue signal. Full retention (1.0) for the first
 * 14 days after its last hard set, then decays with a 28-day half-life toward a floor of 0.5:
 * strength doesn't vanish to zero the way acute fatigue does, and a muscle that's never had a
 * hard set sits at that floor forever rather than reading as "just detrained." This answers
 * "how long since this was really trained," not "how much volume this week."
 *
 * [daysSinceLastHardSet] is null when no hard set has ever been logged for the muscle.
 */
fun strengthRetention(daysSinceLastHardSet: Float?): Float {
    if (daysSinceLastHardSet == null) return RETENTION_FLOOR
    if (daysSinceLastHardSet <= FULL_RETENTION_DAYS) return 1f
    val daysDecaying = daysSinceLastHardSet - FULL_RETENTION_DAYS
    val decay = 0.5.pow(daysDecaying / RETENTION_HALF_LIFE_DAYS).toFloat()
    return RETENTION_FLOOR + (1f - RETENTION_FLOOR) * decay
}
