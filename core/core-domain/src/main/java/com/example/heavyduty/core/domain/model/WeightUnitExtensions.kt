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

/**
 * A kg value converted to [unit] and rounded to what a lifter can actually load or type —
 * 0.5 kg or 1 lb — so a converted suggestion reads "137 lb", not "137.79 lb".
 */
fun Float.kgToRounded(unit: WeightUnit): Float {
    val converted = kgTo(unit)
    return when (unit) {
        WeightUnit.KG -> kotlin.math.round(converted * 2f) / 2f
        WeightUnit.LB -> kotlin.math.round(converted)
    }
}

/** The step for − / + weight buttons: the smallest usual pair of plates in that unit. */
val WeightUnit.loadStep: Float
    get() = when (this) {
        WeightUnit.KG -> 2.5f
        WeightUnit.LB -> 5f
    }

val WeightUnit.label: String
    get() = when (this) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }

fun WeightUnit.other(): WeightUnit = if (this == WeightUnit.KG) WeightUnit.LB else WeightUnit.KG
