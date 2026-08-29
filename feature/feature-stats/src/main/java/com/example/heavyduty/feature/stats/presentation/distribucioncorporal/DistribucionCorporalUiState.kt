package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

import com.example.heavyduty.core.ui.components.BodyGender

enum class BodyMapMode { VOLUME, FATIGUE, STRENGTH }

data class DistribucionCorporalUiState(
    val volumeByMuscle: Map<String, Float> = emptyMap(),
    /** 0 (fresh) .. 1 (maximally fatigued) per muscle — see GetMuscleGroupFatigueUseCase. */
    val fatigueByMuscle: Map<String, Float> = emptyMap(),
    /** 0.5 (floor) .. 1 (fully retained) per muscle — see GetMuscleGroupStrengthRetentionUseCase. */
    val strengthByMuscle: Map<String, Float> = emptyMap(),
    val mode: BodyMapMode = BodyMapMode.VOLUME,
    val gender: BodyGender = BodyGender.MALE,
    val isLoading: Boolean = true,
)

sealed interface DistribucionCorporalUiEvent {
    data class OnModeChange(val mode: BodyMapMode) : DistribucionCorporalUiEvent
}
