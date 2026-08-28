package com.example.heavyduty.core.domain.model

/**
 * The single, shared filter every volume, PR, 1RM and fatigue calculation should apply
 * before doing any math — ramp-up sets carry no training signal for those numbers, but
 * still belong in the session's log. Filter through this instead of re-deriving the
 * exclusion at each call site.
 */
fun List<WorkoutSet>.excludeWarmups(): List<WorkoutSet> = filter { !it.isWarmup }
