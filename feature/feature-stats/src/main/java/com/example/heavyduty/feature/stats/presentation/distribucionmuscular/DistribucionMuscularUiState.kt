package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.components.StatsPeriod
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary

data class DistribucionMuscularUiState(
    val currentWeek: Map<MuscleGroup, Float> = emptyMap(),
    val previousWeek: Map<MuscleGroup, Float> = emptyMap(),
    val selectedPeriod: StatsPeriod = StatsPeriod.LAST_30_DAYS,
    val summary: PeriodSummary = PeriodSummary(),
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false,
)

sealed interface DistribucionMuscularUiEvent {
    data class OnPeriodChange(val period: StatsPeriod) : DistribucionMuscularUiEvent
}
