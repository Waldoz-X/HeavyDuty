package com.example.heavyduty.feature.stats.presentation.mapaactividad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.feature.stats.domain.usecase.GetTrainingActivityHeatmapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MapaActividadViewModel @Inject constructor(
    private val getTrainingActivityHeatmap: GetTrainingActivityHeatmapUseCase,
    private val clock: Clock
) : ViewModel() {

    private data class FrequencySelection(val view: FrequencyView = FrequencyView.MONTH, val offset: Int = 0)

    private val selection = MutableStateFlow(FrequencySelection())

    private val _uiState = MutableStateFlow(MapaActividadUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<MapaActividadUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            combine(getTrainingActivityHeatmap(), selection) { report, selected ->
                report to buildFrequencyPanel(selected.view, selected.offset, clock.nowMillis(), report.dailyTotals)
            }
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { (report, frequency) ->
                    _uiState.update {
                        it.copy(report = report, frequency = frequency, isLoading = false)
                    }
                }
        }
    }

    fun onEvent(event: MapaActividadUiEvent) {
        val frequency = _uiState.value.frequency
        when (event) {
            is MapaActividadUiEvent.OnSelectFrequencyView -> selection.value = FrequencySelection(view = event.view)

            MapaActividadUiEvent.OnPreviousPeriod -> if (frequency?.canGoBack == true) {
                selection.update { it.copy(offset = it.offset - 1) }
            }

            MapaActividadUiEvent.OnNextPeriod -> if (frequency?.canGoForward == true) {
                selection.update { it.copy(offset = it.offset + 1) }
            }
        }
    }
}
