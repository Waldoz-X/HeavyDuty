package com.example.heavyduty.core.common.extensions

/** Formats as a whole number when it has no fractional part, otherwise with one decimal. */
fun Float.formatDecimal(): String =
    if (this == this.toInt().toFloat()) this.toInt().toString() else "%.1f".format(this)

/** Compact large-number formatting for stat displays, e.g. 12500f -> "12.5k". */
fun Float.formatCompact(): String =
    if (this >= 1000f) "${(this / 1000f).formatDecimal()}k" else this.toInt().toString()
