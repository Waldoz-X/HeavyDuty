package com.example.heavyduty.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.feature.dashboard.domain.model.currentWeekRanges
import com.example.heavyduty.feature.dashboard.domain.usecase.GetBodyWeightSummaryUseCase
import com.example.heavyduty.feature.dashboard.domain.usecase.GetCurrentStreakUseCase
import com.example.heavyduty.feature.dashboard.domain.usecase.GetRecentWorkoutUseCase
import com.example.heavyduty.feature.dashboard.domain.usecase.GetWeeklyMuscleVolumeUseCase
import com.example.heavyduty.feature.dashboard.domain.usecase.GetWeeklyScheduleOverviewUseCase
import com.example.heavyduty.feature.dashboard.domain.usecase.GetWeeklySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val calendarRepository: CalendarRepository,
    private val getWeeklySummary: GetWeeklySummaryUseCase,
    private val getWeeklyMuscleVolume: GetWeeklyMuscleVolumeUseCase,
    private val getWeeklyScheduleOverview: GetWeeklyScheduleOverviewUseCase,
    private val getRecentWorkout: GetRecentWorkoutUseCase,
    private val getBodyWeightSummary: GetBodyWeightSummaryUseCase,
    private val getCurrentStreak: GetCurrentStreakUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<DashboardUiState> = _uiState.whileSubscribed(viewModelScope) {
        loadDashboardData()
        loadWeekPlan()
    }

    private val _uiEffect = MutableSharedFlow<DashboardUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private val weekOffset = MutableStateFlow(0)

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.OnStartFreeWorkout -> {
                viewModelScope.launch {
                    _uiEffect.emit(DashboardUiEffect.NavigateToActiveWorkout)
                }
            }
            is DashboardUiEvent.OnChooseRoutine -> {
                viewModelScope.launch {
                    _uiEffect.emit(DashboardUiEffect.NavigateToRoutines)
                }
            }
            is DashboardUiEvent.OnWeekPrev -> changeWeekOffset(-1)
            is DashboardUiEvent.OnWeekNext -> changeWeekOffset(1)
            is DashboardUiEvent.OnStartTodayRoutine -> {
                viewModelScope.launch {
                    _uiEffect.emit(DashboardUiEffect.NavigateToRoutine(event.routineId))
                }
            }
            is DashboardUiEvent.OnOpenLogWeight -> {
                _uiState.update { it.copy(showLogWeightSheet = true) }
            }
            is DashboardUiEvent.OnDismissLogWeight -> {
                _uiState.update { it.copy(showLogWeightSheet = false) }
            }
            is DashboardUiEvent.OnLogWeight -> saveWeight(event.weightKg)
        }
    }

    private fun changeWeekOffset(delta: Int) {
        weekOffset.update { it + delta }
        _uiState.update { it.copy(weekOffset = weekOffset.value) }
    }

    private fun saveWeight(weightKg: Float) {
        viewModelScope.launch {
            bodyWeightRepository.logEntry(weightKg)
            _uiState.update { it.copy(showLogWeightSheet = false) }
        }
    }

    private fun CoroutineScope.loadWeekPlan() {
        launch {
            weekOffset
                .flatMapLatest { offset -> getWeeklyScheduleOverview(offset) }
                .combine(calendarRepository.getWeekSchedule()) { plan, schedule ->
                    _uiState.update {
                        it.copy(
                            weekPlan = plan,
                            plannedDaysThisWeek = schedule.count { entry -> entry.value.isNotEmpty() },
                        )
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { }
        }
    }

    private fun CoroutineScope.loadDashboardData() {
        launch {
            // Queries cover the whole current week and move to the next one when it starts,
            // so workouts logged while the dashboard is kept alive in the back stack show up.
            val weekly = currentWeekRanges(clock::nowMillis).flatMapLatest { week ->
                combine(
                    getWeeklySummary(week),
                    getWeeklyMuscleVolume(week.first, week.last),
                ) { totals, muscleVolume -> totals to muscleVolume }
            }

            combine(
                weekly,
                getRecentWorkout(),
                getBodyWeightSummary(),
                getCurrentStreak(),
            ) { (totals, muscleVolume), recentWorkout, weightSummary, streak ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weeklyWorkoutsCount = totals.workouts,
                        totalTimeSeconds = totals.durationSecs,
                        weeklyVolumeKg = totals.volumeKg,
                        weeklyMuscleVolume = muscleVolume,
                        recentWorkoutName = recentWorkout?.sessionName?.takeIf { name -> name.isNotBlank() },
                        recentWorkoutEndTime = recentWorkout?.endTime,
                        bodyWeightSummary = weightSummary,
                        streakDays = streak,
                    )
                }
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { }
        }
    }
}
