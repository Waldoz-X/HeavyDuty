package com.example.heavyduty.feature.stats.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupVolumeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    private val getMuscleGroupVolume: GetMuscleGroupVolumeUseCase,
) : ViewModel() {

    private val last7Days: List<Long> = buildLast7DaysStartMillis()
    private val selectedDayMillis = MutableStateFlow(last7Days.last())

    private val _uiState = MutableStateFlow(EstadisticasUiState())
    val uiState: StateFlow<EstadisticasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedDayMillis
                .flatMapLatest { dayStart ->
                    getMuscleGroupVolume(dayStart, dayStart + DAY_MILLIS - 1)
                        .map { volume -> dayStart to volume }
                }
                .collect { (selectedDay, volumeByMuscle) ->
                    _uiState.update {
                        it.copy(
                            days = last7Days.map { day -> day.toDayChipUi(isSelected = day == selectedDay) },
                            selectedDayVolumeByMuscle = volumeByMuscle.mapKeys { entry -> entry.key.displayName },
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun onEvent(event: EstadisticasUiEvent) {
        when (event) {
            is EstadisticasUiEvent.OnDaySelected -> selectedDayMillis.value = event.dateMillis
        }
    }
}

private fun buildLast7DaysStartMillis(): List<Long> {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val days = mutableListOf<Long>()
    repeat(7) {
        days.add(0, calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return days
}

private fun Long.toDayChipUi(isSelected: Boolean): DayChipUi {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@toDayChipUi }
    val dayLetter = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "D"
        Calendar.MONDAY -> "L"
        Calendar.TUESDAY -> "M"
        Calendar.WEDNESDAY -> "Mi"
        Calendar.THURSDAY -> "J"
        Calendar.FRIDAY -> "V"
        else -> "S"
    }
    return DayChipUi(
        dateMillis = this,
        dayLetter = dayLetter,
        dayNumber = calendar.get(Calendar.DAY_OF_MONTH).toString(),
        isSelected = isSelected,
    )
}
