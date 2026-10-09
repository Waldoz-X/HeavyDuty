package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.core.ui.components.StatsPeriod
import com.example.heavyduty.core.ui.components.currentAndPreviousRange
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupFatigueUseCase
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupStrengthRetentionUseCase
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupVolumeUseCase
import com.example.heavyduty.feature.stats.domain.usecase.GetPeriodSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DistribucionCorporalViewModel @Inject constructor(
    private val getMuscleGroupVolume: GetMuscleGroupVolumeUseCase,
    private val getMuscleGroupFatigue: GetMuscleGroupFatigueUseCase,
    private val getMuscleGroupStrengthRetention: GetMuscleGroupStrengthRetentionUseCase,
    private val getPeriodSummary: GetPeriodSummaryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistribucionCorporalUiState())
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<DistribucionCorporalUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            _selectedPeriod.flatMapLatest { period ->
                val (currentRange, previousRange) = period.currentAndPreviousRange()
                combine(
                    getMuscleGroupVolume(currentRange.first, currentRange.last),
                    getMuscleGroupFatigue(),
                    getMuscleGroupStrengthRetention(),
                    userPreferencesRepository.bodyDiagramGender,
                    getPeriodSummary(currentRange, previousRange),
                ) { volumeByMuscle, fatigueByMuscle, strengthByMuscle, genderValue, summary ->
                    _uiState.update {
                        it.copy(
                            volumeByMuscle = volumeByMuscle,
                            fatigueByMuscle = fatigueByMuscle,
                            strengthByMuscle = strengthByMuscle,
                            gender = BodyGender.fromStorageValue(genderValue),
                            selectedPeriod = period,
                            summary = summary,
                            isLoading = false,
                        )
                    }
                }
            }.collect { }
        }
    }

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.LAST_30_DAYS)

    fun onEvent(event: DistribucionCorporalUiEvent) {
        when (event) {
            is DistribucionCorporalUiEvent.OnModeChange -> _uiState.update { it.copy(mode = event.mode) }
            is DistribucionCorporalUiEvent.OnPeriodChange -> _selectedPeriod.value = event.period
        }
    }
}
