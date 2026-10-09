package com.example.heavyduty.feature.stats.presentation.pesocorporal

import com.example.heavyduty.feature.stats.domain.usecase.BodyWeightStatsReport

data class BodyWeightStatsUiState(
    val isLoading: Boolean = true,
    val report: BodyWeightStatsReport? = null,
    val loadFailed: Boolean = false,
)
