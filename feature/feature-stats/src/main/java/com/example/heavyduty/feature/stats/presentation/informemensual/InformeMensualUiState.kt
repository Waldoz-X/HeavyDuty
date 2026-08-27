package com.example.heavyduty.feature.stats.presentation.informemensual

import com.example.heavyduty.feature.stats.domain.model.MonthlyReport

data class InformeMensualUiState(
    val report: MonthlyReport = MonthlyReport(),
    val isLoading: Boolean = true,
)
