package com.example.heavyduty.feature.stats.presentation.mapaactividad

import com.example.heavyduty.feature.stats.domain.usecase.ActivityHeatmapStatsReport

data class MapaActividadUiState(
    val report: ActivityHeatmapStatsReport = ActivityHeatmapStatsReport(),
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false,
)
