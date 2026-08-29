package com.example.heavyduty.feature.stats.presentation.mapaactividad

data class MapaActividadUiState(
    /** Local-midnight millis -> intensity 0..1, over the last ~53 weeks. */
    val activityByDay: Map<Long, Float> = emptyMap(),
    val daysTrained: Int = 0,
    val isLoading: Boolean = true,
)
