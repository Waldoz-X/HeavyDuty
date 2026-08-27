package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

data class DistribucionCorporalUiState(
    val volumeByMuscle: Map<String, Float> = emptyMap(),
    val isLoading: Boolean = true,
)
