package com.example.heavyduty.core.domain.model

/** Standard kg plate denominations found at most gyms. */
val STANDARD_PLATES_KG = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)

/** Standard pound plate denominations. */
val STANDARD_PLATES_LB = listOf(45f, 35f, 25f, 10f, 5f, 2.5f)

/** Common bar weights, heaviest (the standard Olympic bar) first. */
fun WeightUnit.barPresets(): List<Float> = when (this) {
    WeightUnit.KG -> listOf(20f, 15f, 10f)
    WeightUnit.LB -> listOf(45f, 35f, 15f)
}

fun WeightUnit.standardPlates(): List<Float> = when (this) {
    WeightUnit.KG -> STANDARD_PLATES_KG
    WeightUnit.LB -> STANDARD_PLATES_LB
}

/**
 * Greedy per-side plate breakdown for [targetWeightKg] on a bar weighing [barWeightKg]: at
 * each step, uses the heaviest available plate that still fits the remaining weight — the
 * same way a lifter actually loads a bar, not an exhaustive search for an exact match. A
 * target at or below the bar's own weight needs no plates at all (empty list); a remainder
 * too small for the smallest denomination is simply left unrepresented rather than rounded.
 */
fun calculatePlatesPerSide(
    targetWeightKg: Float,
    barWeightKg: Float,
    availablePlates: List<Float> = STANDARD_PLATES_KG,
): List<Float> {
    var remainingPerSide = (targetWeightKg - barWeightKg) / 2f
    if (remainingPerSide <= 0f) return emptyList()

    val plates = mutableListOf<Float>()
    for (plate in availablePlates.sortedDescending()) {
        while (remainingPerSide + 0.001f >= plate) {
            plates.add(plate)
            remainingPerSide -= plate
        }
    }
    return plates
}
