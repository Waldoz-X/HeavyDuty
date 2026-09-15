package com.example.heavyduty.core.domain.model

private const val KG_PER_LB = 0.45359237f

/** Converts a kg amount to the given display unit — kg is always the value's canonical unit. */
fun Float.kgTo(unit: WeightUnit): Float = when (unit) {
    WeightUnit.KG -> this
    WeightUnit.LB -> this / KG_PER_LB
}

/** Converts a value already expressed in [unit] back to kg, for storage. */
fun Float.toKgFrom(unit: WeightUnit): Float = when (unit) {
    WeightUnit.KG -> this
    WeightUnit.LB -> this * KG_PER_LB
}
