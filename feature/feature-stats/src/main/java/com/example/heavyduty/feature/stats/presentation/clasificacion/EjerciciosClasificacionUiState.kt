package com.example.heavyduty.feature.stats.presentation.clasificacion

import com.example.heavyduty.feature.stats.domain.model.ExerciseRanking

data class EjerciciosClasificacionUiState(
    val rankings: List<ExerciseRanking> = emptyList(),
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false,
)
