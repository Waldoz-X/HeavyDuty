package com.example.heavyduty.feature.stats.presentation.series

data class SeriesPorMusculoUiState(
    val data: List<Pair<String, Float>> = emptyList(),
    val isLoading: Boolean = true,
)
