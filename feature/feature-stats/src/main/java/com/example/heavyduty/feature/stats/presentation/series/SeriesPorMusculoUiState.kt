package com.example.heavyduty.feature.stats.presentation.series

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.components.StatsPeriod

data class SeriesPorMusculoUiState(
    /** Working sets per muscle, largest first — fractional where a set credits several muscles. */
    val data: List<Pair<MuscleGroup, Float>> = emptyList(),
    val selectedPeriod: StatsPeriod = StatsPeriod.LAST_7_DAYS,
    val isLoading: Boolean = true,
)

sealed interface SeriesPorMusculoUiEvent {
    data class OnPeriodChange(val period: StatsPeriod) : SeriesPorMusculoUiEvent
}
