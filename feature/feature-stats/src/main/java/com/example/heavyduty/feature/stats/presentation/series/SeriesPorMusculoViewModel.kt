package com.example.heavyduty.feature.stats.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.ui.components.StatsPeriod
import com.example.heavyduty.core.ui.components.currentAndPreviousRange
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupSetCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Working sets per muscle group over a selectable period. The range runs through the end of
 * today (see [currentAndPreviousRange]) — it used to stop at the moment the screen opened, so
 * sets logged afterwards were missing, and it only ever covered "this week".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesPorMusculoViewModel @Inject constructor(
    private val getMuscleGroupSetCount: GetMuscleGroupSetCountUseCase,
    private val clock: Clock
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(StatsPeriod.LAST_7_DAYS)

    private val _uiState = MutableStateFlow(SeriesPorMusculoUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<SeriesPorMusculoUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            selectedPeriod
                .flatMapLatest { period ->
                    val (range, _) = period.currentAndPreviousRange(clock.nowMillis())
                    getMuscleGroupSetCount(range.first, range.last).map { period to it }
                }
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { (period, countByMuscle) ->
                    _uiState.update {
                        it.copy(
                            data = countByMuscle.entries
                                .filter { entry -> entry.value > 0f }
                                .sortedByDescending { entry -> entry.value }
                                .map { entry -> entry.key to entry.value },
                            selectedPeriod = period,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: SeriesPorMusculoUiEvent) {
        when (event) {
            is SeriesPorMusculoUiEvent.OnPeriodChange -> selectedPeriod.value = event.period
        }
    }
}
