package com.example.heavyduty.feature.stats.presentation.mapaactividad

import com.example.heavyduty.feature.stats.domain.usecase.ActivityHeatmapStatsReport

data class MapaActividadUiState(
    val report: ActivityHeatmapStatsReport = ActivityHeatmapStatsReport(),
    /** The frequency card's selected view and period; null until the history has loaded. */
    val frequency: FrequencyPanel? = null,
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false
)

sealed interface MapaActividadUiEvent {
    /** Switch between week, month and year — always landing on the current one. */
    data class OnSelectFrequencyView(val view: FrequencyView) : MapaActividadUiEvent

    data object OnPreviousPeriod : MapaActividadUiEvent

    data object OnNextPeriod : MapaActividadUiEvent
}
