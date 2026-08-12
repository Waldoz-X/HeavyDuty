package com.example.heavyduty.feature.timer.domain.model

/**
 * Available timer modes for the timer feature.
 */
enum class TimerMode(val displayName: String, val description: String) {
    REST("Rest Timer", "Countdown between sets"),
    STOPWATCH("Stopwatch", "Count up freely"),
    AMRAP("AMRAP", "As Many Reps As Possible"),
    EMOM("EMOM", "Every Minute On the Minute"),
    TABATA("Tabata", "20s work / 10s rest"),
}
