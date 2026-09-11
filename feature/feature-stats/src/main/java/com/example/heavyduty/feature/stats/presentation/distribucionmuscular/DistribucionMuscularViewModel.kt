package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupVolumeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val MAX_AXES = 8

@HiltViewModel
class DistribucionMuscularViewModel @Inject constructor(
    private val getMuscleGroupVolume: GetMuscleGroupVolumeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistribucionMuscularUiState())
    val uiState: StateFlow<DistribucionMuscularUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfWeek = calendar.timeInMillis
            val now = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val startOfPreviousWeek = calendar.timeInMillis

            combine(
                getMuscleGroupVolume(startOfWeek, now),
                getMuscleGroupVolume(startOfPreviousWeek, startOfWeek - 1),
            ) { current, previous ->
                val topKeys = (current.keys + previous.keys)
                    .distinct()
                    .sortedByDescending { (current[it] ?: 0f) + (previous[it] ?: 0f) }
                    .take(MAX_AXES)
                val currentMap = topKeys.associateWith { current[it] ?: 0f }
                val previousMap = topKeys.associateWith { previous[it] ?: 0f }
                currentMap to previousMap
            }.collect { (currentMap, previousMap) ->
                _uiState.update {
                    it.copy(currentWeek = currentMap, previousWeek = previousMap, isLoading = false)
                }
            }
        }
    }
}
