package com.example.heavyduty.core.domain.model

/**
 * Perceived effort logged on a set — write-only and purely descriptive. Nothing in
 * [nextPrescription] or [estimatedOneRepMax] reads it.
 *
 * Whichever scale was used when the set was saved is kept as-is forever: if the user later
 * switches their preferred scale in settings, older sets are never rewritten to the new one.
 * [displayRir]/[displayRpe] do the RIR = 10 − RPE conversion, but only for showing an old
 * RIR set on an RPE-scale screen (or vice versa) — never for storage.
 */
sealed class Effort {
    data class Rir(val value: Int) : Effort()
    data class Rpe(val value: Float) : Effort()
}

fun Effort.displayRir(): Int = when (this) {
    is Effort.Rir -> value
    is Effort.Rpe -> Math.round(10f - value)
}

fun Effort.displayRpe(): Float = when (this) {
    is Effort.Rir -> (10 - value).toFloat()
    is Effort.Rpe -> value
}
