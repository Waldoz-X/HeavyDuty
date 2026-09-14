package com.example.heavyduty.feature.stats.presentation.mapaactividad

import com.example.heavyduty.feature.stats.domain.usecase.ActivityHeatmapStatsReport

data class MapaActividadUiState(
    val report: ActivityHeatmapStatsReport = ActivityHeatmapStatsReport(),
    val isLoading: Boolean = true,
)
