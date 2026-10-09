package com.example.heavyduty.feature.stats.presentation.principales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.feature.stats.domain.usecase.GetTopExercisesByFrequencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EjerciciosPrincipalesViewModel @Inject constructor(
    private val getTopExercisesByFrequency: GetTopExercisesByFrequencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EjerciciosPrincipalesUiState())
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<EjerciciosPrincipalesUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            getTopExercisesByFrequency().collect { exercises ->
                _uiState.update { it.copy(exercises = exercises, isLoading = false) }
            }
        }
    }

}
