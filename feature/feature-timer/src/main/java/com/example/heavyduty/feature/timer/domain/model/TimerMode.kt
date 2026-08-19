package com.example.heavyduty.feature.timer.domain.model

/**
 * Available timer modes for the timer feature.
 */
enum class TimerMode(val displayName: String, val description: String) {
    REST("Descanso", "Cuenta regresiva entre series"),
    STOPWATCH("Cronómetro", "Cuenta libremente hacia adelante"),
    AMRAP("AMRAP", "Tantas repeticiones como sea posible"),
    EMOM("EMOM", "Cada minuto en el minuto"),
    TABATA("Tabata", "20s trabajo / 10s descanso"),
}
