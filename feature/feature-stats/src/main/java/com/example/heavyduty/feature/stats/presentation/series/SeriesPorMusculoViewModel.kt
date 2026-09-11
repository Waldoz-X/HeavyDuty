package com.example.heavyduty.feature.stats.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupSetCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SeriesPorMusculoViewModel @Inject constructor(
    private val getMuscleGroupSetCount: GetMuscleGroupSetCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesPorMusculoUiState())
    val uiState: StateFlow<SeriesPorMusculoUiState> = _uiState.asStateFlow()

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

            getMuscleGroupSetCount(startOfWeek, now).collect { countByMuscle ->
                _uiState.update {
                    it.copy(
                        data = countByMuscle.entries
                            .sortedByDescending { entry -> entry.value }
                            .map { entry -> entry.key to entry.value.toFloat() },
                        isLoading = false,
                    )
                }
            }
        }
    }
}
