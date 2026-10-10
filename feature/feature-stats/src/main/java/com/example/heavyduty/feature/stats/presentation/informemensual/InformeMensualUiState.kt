package com.example.heavyduty.feature.stats.presentation.informemensual

import com.example.heavyduty.feature.stats.domain.model.MonthlyReport

data class InformeMensualUiState(
    val report: MonthlyReport = MonthlyReport(),
    val currentReferenceMillis: Long = System.currentTimeMillis(),
    val hasNextMonth: Boolean = false,
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false
)
