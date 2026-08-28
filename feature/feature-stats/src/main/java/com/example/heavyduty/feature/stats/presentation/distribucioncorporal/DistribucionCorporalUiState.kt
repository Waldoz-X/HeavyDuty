package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.feature.stats.domain.model.FatigueState

enum class BodyMapMode { VOLUME, FATIGUE }

data class DistribucionCorporalUiState(
    val volumeByMuscle: Map<String, Float> = emptyMap(),
    val fatigueByMuscle: Map<String, FatigueState> = emptyMap(),
    val mode: BodyMapMode = BodyMapMode.VOLUME,
    val gender: BodyGender = BodyGender.MALE,
    val isLoading: Boolean = true,
)

sealed interface DistribucionCorporalUiEvent {
    data class OnModeChange(val mode: BodyMapMode) : DistribucionCorporalUiEvent
}
