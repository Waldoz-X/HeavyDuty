package com.example.heavyduty.core.domain.model

/**
 * Whether [weightKg] is so far above [bestKg] — the heaviest this exercise has been lifted — that
 * it's more likely a typo (609 for 60.9) than a record: more than half again *and* more than
 * 20 kg over it. Both, so light weights can still double (4 → 8 kg dumbbells) and heavy ones
 * can still jump 20 kg. With nothing to compare against there's no telling, so never.
 */
fun isUnusuallyHeavy(weightKg: Float?, bestKg: Float?): Boolean {
    if (weightKg == null || bestKg == null || bestKg <= 0f) return false
    return weightKg > bestKg * UNUSUAL_FACTOR && weightKg > bestKg + UNUSUAL_MARGIN_KG
}

private const val UNUSUAL_FACTOR = 1.5f
private const val UNUSUAL_MARGIN_KG = 20f
