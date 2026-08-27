package com.example.heavyduty.feature.stats.presentation.clasificacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetExercisePersonalRecordsListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EjerciciosClasificacionViewModel @Inject constructor(
    private val getExercisePersonalRecordsList: GetExercisePersonalRecordsListUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EjerciciosClasificacionUiState())
    val uiState: StateFlow<EjerciciosClasificacionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getExercisePersonalRecordsList().collect { rankings ->
                _uiState.update { it.copy(rankings = rankings, isLoading = false) }
            }
        }
    }
}
