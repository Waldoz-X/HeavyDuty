package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupVolumeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DistribucionCorporalViewModel @Inject constructor(
    private val getMuscleGroupVolume: GetMuscleGroupVolumeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistribucionCorporalUiState())
    val uiState: StateFlow<DistribucionCorporalUiState> = _uiState.asStateFlow()

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

            getMuscleGroupVolume(startOfWeek, now).collect { volumeByMuscle ->
                _uiState.update {
                    it.copy(
                        volumeByMuscle = volumeByMuscle.mapKeys { entry -> entry.key.displayName },
                        isLoading = false,
                    )
                }
            }
        }
    }
}
