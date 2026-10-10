package com.example.heavyduty.feature.stats.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.core.ui.components.StatsPeriod
import com.example.heavyduty.core.ui.components.currentAndPreviousRange
import com.example.heavyduty.feature.stats.domain.usecase.GetMuscleGroupVolumeUseCase
import com.example.heavyduty.feature.stats.domain.usecase.GetPeriodSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    private val getMuscleGroupVolume: GetMuscleGroupVolumeUseCase,
    private val getPeriodSummary: GetPeriodSummaryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) : ViewModel() {

    private val last7Days: List<Long> = buildLast7DaysStartMillis(clock.nowMillis())
    private val selectedDayMillis = MutableStateFlow(last7Days.last())

    private val _uiState = MutableStateFlow(EstadisticasUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<EstadisticasUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            selectedDayMillis
                .flatMapLatest { dayStart ->
                    getMuscleGroupVolume(dayStart, nextDayStart(dayStart) - 1)
                        .map { volume -> dayStart to volume }
                }
                .combine(userPreferencesRepository.bodyDiagramGender) { (selectedDay, volumeByMuscle), genderValue ->
                    _uiState.update {
                        it.copy(
                            days = last7Days.map { day -> day.toDayChipUi(isSelected = day == selectedDay) },
                            selectedDayVolumeByMuscle = volumeByMuscle,
                            gender = BodyGender.fromStorageValue(genderValue),
                            isLoading = false
                        )
                    }
                }
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { }
        }
        launch {
            val (current, previous) = StatsPeriod.LAST_30_DAYS.currentAndPreviousRange(clock.nowMillis())
            getPeriodSummary(current, previous)
                .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { summary ->
                    _uiState.update { it.copy(summary = summary) }
                }
        }
    }

    fun onEvent(event: EstadisticasUiEvent) {
        when (event) {
            is EstadisticasUiEvent.OnDaySelected -> selectedDayMillis.value = event.dateMillis
        }
    }
}

private fun buildLast7DaysStartMillis(now: Long): List<Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
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
    val dayLetter = SimpleDateFormat("EE", Locale.getDefault())
        .format(calendar.time)
        .take(2)
        .replaceFirstChar { it.uppercase() }
    return DayChipUi(
        dateMillis = this,
        dayLetter = dayLetter,
        dayNumber = calendar.get(Calendar.DAY_OF_MONTH).toString(),
        isSelected = isSelected
    )
}

/** Midnight after [dayStart] — not dayStart + 24h, which is off by an hour on DST days. */
private fun nextDayStart(dayStart: Long): Long = Calendar.getInstance().apply {
    timeInMillis = dayStart
    add(Calendar.DAY_OF_YEAR, 1)
}.timeInMillis
