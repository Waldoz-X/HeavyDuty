package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.core.ui.components.StatsPeriod
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary

enum class BodyMapMode { VOLUME, FATIGUE, STRENGTH }

data class DistribucionCorporalUiState(
    val volumeByMuscle: Map<MuscleGroup, Float> = emptyMap(),
    /** 0 (fresh) .. 1 (maximally fatigued) per muscle — see GetMuscleGroupFatigueUseCase. */
    val fatigueByMuscle: Map<MuscleGroup, Float> = emptyMap(),
    /** 0.5 (floor) .. 1 (fully retained) per muscle — see GetMuscleGroupStrengthRetentionUseCase. */
    val strengthByMuscle: Map<MuscleGroup, Float> = emptyMap(),
    val mode: BodyMapMode = BodyMapMode.VOLUME,
    val gender: BodyGender = BodyGender.MALE,
    val selectedPeriod: StatsPeriod = StatsPeriod.LAST_30_DAYS,
    val summary: PeriodSummary = PeriodSummary(),
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false
)

sealed interface DistribucionCorporalUiEvent {
    data class OnModeChange(val mode: BodyMapMode) : DistribucionCorporalUiEvent
    data class OnPeriodChange(val period: StatsPeriod) : DistribucionCorporalUiEvent
}
