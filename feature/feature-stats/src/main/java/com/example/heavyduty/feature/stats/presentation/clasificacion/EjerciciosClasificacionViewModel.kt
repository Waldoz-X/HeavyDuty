package com.example.heavyduty.feature.stats.presentation.clasificacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.feature.stats.domain.usecase.GetExercisePersonalRecordsListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EjerciciosClasificacionViewModel @Inject constructor(
    private val getExercisePersonalRecordsList: GetExercisePersonalRecordsListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EjerciciosClasificacionUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<EjerciciosClasificacionUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            getExercisePersonalRecordsList()
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { rankings ->
                    _uiState.update { it.copy(rankings = rankings, isLoading = false) }
                }
        }
    }
}
