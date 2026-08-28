package com.example.heavyduty.feature.stats.domain.model

/**
 * How recently a muscle group was trained, as a simplified 3-state traffic light.
 *
 * Unlike an intensity-weighted fatigue model (which would need per-set effort data we
 * don't track, e.g. RIR/RPE), this is purely recency-based: how many days have passed
 * since the last completed set for that muscle group.
 */
enum class FatigueState {
    /** Trained within the last day — still deep in recovery. */
    FATIGUED,

    /** Trained 2–3 days ago — partway through a typical recovery window. */
    RECOVERING,

    /** Trained 4+ days ago, or never — recovered (or nothing to recover from). */
    READY,
}
