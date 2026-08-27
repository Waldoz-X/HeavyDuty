package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

data class DistribucionMuscularUiState(
    val currentWeek: Map<String, Float> = emptyMap(),
    val previousWeek: Map<String, Float> = emptyMap(),
    val isLoading: Boolean = true,
)
