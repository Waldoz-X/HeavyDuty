package com.example.heavyduty.feature.stats.presentation.pesocorporal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.feature.stats.domain.usecase.BodyWeightHistoryItem
import com.example.heavyduty.feature.stats.domain.usecase.GetBodyWeightStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BodyWeightStatsViewModel @Inject constructor(
    private val getBodyWeightStatsUseCase: GetBodyWeightStatsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val bodyWeightRepository: BodyWeightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyWeightStatsUiState())
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<BodyWeightStatsUiState> = _uiState.whileSubscribed(viewModelScope) {
        loadStats()
    }

    private fun CoroutineScope.loadStats() {
        launch {
            getBodyWeightStatsUseCase()
                .catch {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { report ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            loadFailed = false,
                        )
                    }
                }
        }
    }

    fun updateGoalWeight(kg: Float?) {
        viewModelScope.launch {
            userPreferencesRepository.setGoalWeightKg(kg)
        }
    }

    fun logWeight(kg: Float) {
        viewModelScope.launch {
            bodyWeightRepository.logEntry(kg)
        }
    }

    fun deleteWeight(entry: BodyWeightHistoryItem) {
        viewModelScope.launch {
            bodyWeightRepository.deleteEntry(
                BodyWeightEntry(
                    id = entry.id,
                    weightKg = entry.weightKg,
                    loggedAt = entry.loggedAt,
                )
            )
        }
    }
}
