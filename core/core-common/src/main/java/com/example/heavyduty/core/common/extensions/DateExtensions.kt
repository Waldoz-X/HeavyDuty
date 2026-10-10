package com.example.heavyduty.core.common.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Extension functions for date and time formatting.
 */

fun Long.toFormattedDate(pattern: String = "dd MMM yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Int.toDurationString(): String {
    val hours = TimeUnit.SECONDS.toHours(this.toLong())
    val minutes = TimeUnit.SECONDS.toMinutes(this.toLong()) % 60
    val seconds = this % 60

    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        else -> String.format(Locale.getDefault(), "0:%02d", seconds)
    }
}

fun Float.toWeightString(): String = if (this % 1.0f == 0f) {
    this.toInt().toString()
} else {
    String.format(Locale.getDefault(), "%.1f", this)
}
