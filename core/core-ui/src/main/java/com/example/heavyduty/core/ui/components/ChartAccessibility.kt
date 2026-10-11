package com.example.heavyduty.core.ui.components

import com.example.heavyduty.core.domain.model.MuscleGroup
import kotlin.math.roundToInt

// What the charts read aloud to a screen reader. They're drawn on a Canvas, which on its own
// says nothing; these turn their data into a sentence.

/** Each part's share of the whole, largest first, as whole percentages; parts at zero are left out. */
fun shareOfTotal(data: Map<String, Float>): List<Pair<String, Int>> {
    val total = data.values.filter { it > 0f }.sum()
    if (total <= 0f) return emptyList()
    return data.entries
        .filter { it.value > 0f }
        .sortedByDescending { it.value }
        .map { (label, value) -> label to (value / total * PERCENT).roundToInt() }
}

/** The muscles a body map highlights, most worked first. */
fun workedMuscles(values: Map<MuscleGroup, Float>): List<MuscleGroup> =
    values.filterValues { it > 0f }.entries.sortedByDescending { it.value }.map { it.key }

private const val PERCENT = 100
