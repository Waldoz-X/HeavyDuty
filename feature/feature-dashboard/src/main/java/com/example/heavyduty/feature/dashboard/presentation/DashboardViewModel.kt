package com.example.heavyduty.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<DashboardUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        loadDashboardData()
    }

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
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllSessions(),
                workoutRepository.getAllCompletedSets(),
            ) { sessions, sets -> sessions to sets }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (sessions, sets) ->
                    val completedSessions = sessions.filter { it.endTime != null }.sortedByDescending { it.endTime }

                    // Calculate weekly stats
                    val now = Calendar.getInstance()
                    now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
                    now.set(Calendar.HOUR_OF_DAY, 0)
                    now.set(Calendar.MINUTE, 0)
                    now.set(Calendar.SECOND, 0)
                    now.set(Calendar.MILLISECOND, 0)
                    val startOfWeek = now.timeInMillis

                    val weeklyWorkouts = completedSessions.filter { it.startTime >= startOfWeek }
                    val weeklyCount = weeklyWorkouts.size
                    val totalTime = weeklyWorkouts.sumOf {
                        if (it.endTime != null) ((it.endTime!! - it.startTime) / 1000).toInt() else 0
                    }
                    val weeklyVolume = sets
                        .filter { (it.completedAt ?: 0L) >= startOfWeek }
                        .sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }
                        .toFloat()

                    val recentWorkout = completedSessions.firstOrNull()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weeklyWorkoutsCount = weeklyCount,
                            totalTimeSeconds = totalTime,
                            weeklyVolumeKg = weeklyVolume,
                            recentWorkoutName = recentWorkout?.sessionName?.takeIf { it.isNotBlank() } ?: "Entrenamiento",
                            recentWorkoutTime = recentWorkout?.endTime?.let { time ->
                                // Simple relative time formatting or date
                                val diff = System.currentTimeMillis() - time
                                val days = diff / (1000 * 60 * 60 * 24)
                                when {
                                    days == 0L -> "Hoy"
                                    days == 1L -> "Ayer"
                                    else -> "Hace $days días"
                                }
                            }
                        )
                    }
                }
        }
    }
}
