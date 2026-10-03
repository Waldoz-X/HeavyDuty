package com.example.heavyduty.core.common.extensions

import java.util.Locale

/** Formats as a whole number when it has no fractional part, otherwise with one decimal. */
fun Float.formatDecimal(): String =
    if (this == this.toInt().toFloat()) this.toInt().toString() else formatOneDecimal()

/**
 * One decimal with a point ("62.5") whatever the phone's language. The number fields take a
 * point, so shown and typed values match; "%.1f".format() used the locale and showed "62,5".
 */
fun Float.formatOneDecimal(): String = String.format(Locale.US, "%.1f", this)

/** Compact large-number formatting for stat displays, e.g. 12500f -> "12.5k". */
fun Float.formatCompact(): String =
    if (this >= 1000f) "${(this / 1000f).formatDecimal()}k" else this.toInt().toString()
