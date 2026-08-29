package com.example.heavyduty.feature.stats.presentation.mapaactividad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetTrainingActivityHeatmapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapaActividadViewModel @Inject constructor(
    private val getTrainingActivityHeatmap: GetTrainingActivityHeatmapUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapaActividadUiState())
    val uiState: StateFlow<MapaActividadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTrainingActivityHeatmap().collect { activityByDay ->
                _uiState.update {
                    it.copy(activityByDay = activityByDay, daysTrained = activityByDay.size, isLoading = false)
                }
            }
        }
    }
}
