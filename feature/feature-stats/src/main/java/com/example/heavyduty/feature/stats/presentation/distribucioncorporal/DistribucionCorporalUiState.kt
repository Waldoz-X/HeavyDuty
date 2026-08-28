package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

import com.example.heavyduty.core.ui.components.BodyGender

data class DistribucionCorporalUiState(
    val volumeByMuscle: Map<String, Float> = emptyMap(),
    val gender: BodyGender = BodyGender.MALE,
    val isLoading: Boolean = true,
)
