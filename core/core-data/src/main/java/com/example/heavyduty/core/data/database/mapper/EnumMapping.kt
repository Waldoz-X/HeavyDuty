package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.domain.model.WeightUnit

/*
 * Enums are stored by name. Reading one the app doesn't know — a value written by a newer
 * version, a bad import, a hand-edited database — falls back to a safe default instead of
 * throwing, which would crash every screen that lists that row, every time.
 */
internal inline fun <reified E : Enum<E>> String.toEnumOr(default: E): E = enumValues<E>().firstOrNull { it.name == this } ?: default

internal fun String?.toWeightUnit(): WeightUnit? = this?.let { name -> WeightUnit.entries.firstOrNull { it.name == name } }
