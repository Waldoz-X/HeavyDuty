package com.example.heavyduty.feature.stats.presentation.pesocorporal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetBodyWeightStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BodyWeightStatsViewModel @Inject constructor(
    private val getBodyWeightStatsUseCase: GetBodyWeightStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyWeightStatsUiState())
    val uiState: StateFlow<BodyWeightStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            getBodyWeightStatsUseCase()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { report ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            error = null,
                        )
                    }
                }
        }
    }
}
