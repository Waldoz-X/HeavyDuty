package com.example.heavyduty.feature.stats.presentation.series

import com.example.heavyduty.core.domain.model.MuscleGroup

data class SeriesPorMusculoUiState(
    val data: List<Pair<MuscleGroup, Float>> = emptyList(),
    val isLoading: Boolean = true,
)
