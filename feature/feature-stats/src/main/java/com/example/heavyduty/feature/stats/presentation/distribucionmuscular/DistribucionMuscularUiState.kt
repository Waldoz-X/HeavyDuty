package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

import com.example.heavyduty.core.domain.model.MuscleGroup

data class DistribucionMuscularUiState(
    val currentWeek: Map<MuscleGroup, Float> = emptyMap(),
    val previousWeek: Map<MuscleGroup, Float> = emptyMap(),
    val isLoading: Boolean = true,
)
