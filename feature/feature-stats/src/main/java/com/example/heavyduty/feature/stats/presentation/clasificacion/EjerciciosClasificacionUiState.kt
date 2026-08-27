package com.example.heavyduty.feature.stats.presentation.clasificacion

import com.example.heavyduty.feature.stats.domain.model.ExerciseRanking

data class EjerciciosClasificacionUiState(
    val rankings: List<ExerciseRanking> = emptyList(),
    val isLoading: Boolean = true,
)
