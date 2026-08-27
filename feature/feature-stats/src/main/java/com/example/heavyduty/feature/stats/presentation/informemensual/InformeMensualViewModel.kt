package com.example.heavyduty.feature.stats.presentation.informemensual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InformeMensualViewModel @Inject constructor(
    private val getMonthlyReport: GetMonthlyReportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InformeMensualUiState())
    val uiState: StateFlow<InformeMensualUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthlyReport().collect { report ->
                _uiState.update { it.copy(report = report, isLoading = false) }
            }
        }
    }
}
