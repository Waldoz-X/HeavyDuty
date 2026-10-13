package com.example.heavyduty.core.common.extensions

import java.util.Locale
import kotlin.math.roundToInt

/**
 * A whole number when it has no fractional part, otherwise one decimal. Rounded first: a sum of
 * fractions such as 11.999997 shows "12", not "12.0".
 */
fun Float.formatDecimal(): String {
    val rounded = (this * 10).roundToInt() / 10f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.formatOneDecimal()
}

/**
 * One decimal with a point ("62.5") whatever the phone's language. The number fields take a
 * point, so shown and typed values match; "%.1f".format() used the locale and showed "62,5".
 */
fun Float.formatOneDecimal(): String = String.format(Locale.US, "%.1f", this)

/** Compact large-number formatting for stat displays, e.g. 12500f -> "12.5k". */
fun Float.formatCompact(): String = if (this >= THOUSAND) "${(this / THOUSAND).formatDecimal()}k" else this.toInt().toString()

private const val THOUSAND = 1000f
