package com.example.heavyduty.feature.stats.presentation.principales

import com.example.heavyduty.feature.stats.domain.model.ExerciseFrequency

data class EjerciciosPrincipalesUiState(
    val exercises: List<ExerciseFrequency> = emptyList(),
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false,
)
