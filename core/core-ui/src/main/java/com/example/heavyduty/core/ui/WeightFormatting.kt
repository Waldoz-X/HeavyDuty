package com.example.heavyduty.core.ui

import com.example.heavyduty.core.common.extensions.formatOneDecimal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.heavyduty.core.common.extensions.formatCompact
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.model.toKgFrom

/**
 * The user's preferred unit for totals and summaries (volume, records, body weight), provided
 * once at the app root. Stored weights are always kg; this only affects how they're shown.
 * Single sets are shown in the unit they were logged in instead — see WorkoutSet.weightUnit.
 */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** A total such as volume: "12.5k kg" / "27.6k lb". */
@Composable
@ReadOnlyComposable
fun Float.formatTotalWeight(): String {
    val unit = LocalWeightUnit.current
    return "${kgTo(unit).formatCompact()} ${unit.label}"
}

/** A single weight to one decimal, without the unit: "72.5" (kg) / "159.8" (lb). */
@Composable
@ReadOnlyComposable
fun Float.formatWeightValue(): String = kgTo(LocalWeightUnit.current).formatOneDecimal()

/** A single weight to one decimal with its unit: "72.5 kg" / "159.8 lb". */
@Composable
@ReadOnlyComposable
fun Float.formatWeight(): String = "${formatWeightValue()} ${LocalWeightUnit.current.label}"

/** The preferred unit's label, for inputs and axis titles: "kg" / "lb". */
@Composable
@ReadOnlyComposable
fun weightUnitLabel(): String = LocalWeightUnit.current.label

/** A value typed in the preferred unit, converted to kg for storage. */
fun Float.fromPreferredUnit(unit: WeightUnit): Float = toKgFrom(unit)
