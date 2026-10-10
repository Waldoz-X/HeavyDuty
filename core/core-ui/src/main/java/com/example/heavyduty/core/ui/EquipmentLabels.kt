package com.example.heavyduty.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.heavyduty.core.common.R

/** Catalog equipment values (English, as stored) → their label in the app's language. */
private val EQUIPMENT_LABELS: Map<String, Int> = mapOf(
    "assisted" to R.string.equipment_assisted,
    "band" to R.string.equipment_band,
    "barbell" to R.string.equipment_barbell,
    "body weight" to R.string.equipment_body_weight,
    "bosu ball" to R.string.equipment_bosu_ball,
    "cable" to R.string.equipment_cable,
    "dumbbell" to R.string.equipment_dumbbell,
    "elliptical machine" to R.string.equipment_elliptical_machine,
    "ez barbell" to R.string.equipment_ez_barbell,
    "hammer" to R.string.equipment_hammer,
    "kettlebell" to R.string.equipment_kettlebell,
    "leverage machine" to R.string.equipment_leverage_machine,
    "medicine ball" to R.string.equipment_medicine_ball,
    "olympic barbell" to R.string.equipment_olympic_barbell,
    "resistance band" to R.string.equipment_resistance_band,
    "roller" to R.string.equipment_roller,
    "rope" to R.string.equipment_rope,
    "skierg machine" to R.string.equipment_skierg_machine,
    "sled machine" to R.string.equipment_sled_machine,
    "smith machine" to R.string.equipment_smith_machine,
    "stability ball" to R.string.equipment_stability_ball,
    "stationary bike" to R.string.equipment_stationary_bike,
    "stepmill machine" to R.string.equipment_stepmill_machine,
    "tire" to R.string.equipment_tire,
    "trap bar" to R.string.equipment_trap_bar,
    "upper body ergometer" to R.string.equipment_upper_body_ergometer,
    "weighted" to R.string.equipment_weighted,
    "wheel roller" to R.string.equipment_wheel_roller
)

/**
 * A raw catalog equipment value (e.g. "body weight") as shown to the user, in the app's
 * language. Values the catalog doesn't know (custom exercises) are shown as typed.
 */
@Composable
@ReadOnlyComposable
fun equipmentLabel(rawEquipment: String): String {
    val res = EQUIPMENT_LABELS[rawEquipment.trim().lowercase()]
    return when {
        res != null -> stringResource(res)
        rawEquipment.isBlank() -> stringResource(R.string.equipment_none)
        else -> rawEquipment
    }
}
