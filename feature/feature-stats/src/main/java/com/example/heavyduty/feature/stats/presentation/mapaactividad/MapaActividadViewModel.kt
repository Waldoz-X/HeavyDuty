package com.example.heavyduty.feature.stats.presentation.mapaactividad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.feature.stats.domain.usecase.GetTrainingActivityHeatmapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapaActividadViewModel @Inject constructor(
    private val getTrainingActivityHeatmap: GetTrainingActivityHeatmapUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapaActividadUiState())
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<MapaActividadUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            getTrainingActivityHeatmap()
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { report ->
                    _uiState.update {
                        it.copy(report = report, isLoading = false)
                    }
                }
        }
    }

}
