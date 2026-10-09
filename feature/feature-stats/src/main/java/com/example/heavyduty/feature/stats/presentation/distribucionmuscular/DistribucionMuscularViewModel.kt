package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.ui.components.StatsPeriod
import com.example.heavyduty.core.ui.components.currentAndPreviousRange
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupVolumeUseCase
import com.example.heavyduty.feature.stats.domain.usecase.GetPeriodSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_AXES = 8

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DistribucionMuscularViewModel @Inject constructor(
    private val getMuscleGroupVolume: GetMuscleGroupVolumeUseCase,
    private val getPeriodSummary: GetPeriodSummaryUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistribucionMuscularUiState())
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<DistribucionMuscularUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            _selectedPeriod.flatMapLatest { period ->
                val (currentRange, previousRange) = period.currentAndPreviousRange(clock.nowMillis())
                combine(
                    getMuscleGroupVolume(currentRange.first, currentRange.last),
                    getMuscleGroupVolume(previousRange.first, previousRange.last),
                    getPeriodSummary(currentRange, previousRange),
                ) { current, previous, summary ->
                    val topKeys = (current.keys + previous.keys)
                        .distinct()
                        .sortedByDescending { (current[it] ?: 0f) + (previous[it] ?: 0f) }
                        .take(MAX_AXES)
                    val currentMap = topKeys.associateWith { current[it] ?: 0f }
                    val previousMap = topKeys.associateWith { previous[it] ?: 0f }
                    Triple(currentMap, previousMap, summary)
                }
            }.catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { (currentMap, previousMap, summary) ->
                _uiState.update {
                    it.copy(
                        currentWeek = currentMap,
                        previousWeek = previousMap,
                        selectedPeriod = _selectedPeriod.value,
                        summary = summary,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.LAST_30_DAYS)

    fun onEvent(event: DistribucionMuscularUiEvent) {
        when (event) {
            is DistribucionMuscularUiEvent.OnPeriodChange -> _selectedPeriod.value = event.period
        }
    }
}
