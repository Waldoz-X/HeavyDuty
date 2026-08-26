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

import com.example.heavyduty.core.domain.repository.ProgramRepository
import com.example.heavyduty.feature.dashboard.domain.usecase.GetWeeklyMuscleVolumeUseCase

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository,
    private val getWeeklyMuscleVolume: GetWeeklyMuscleVolumeUseCase,
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
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.timeInMillis
            val now = System.currentTimeMillis()

            data class DashboardData(
                val weeklySessions: List<com.example.heavyduty.core.domain.model.WorkoutSession>,
                val weeklySets: List<com.example.heavyduty.core.domain.model.WorkoutSet>,
                val activeProgram: com.example.heavyduty.core.domain.model.Program?,
                val allSessions: List<com.example.heavyduty.core.domain.model.WorkoutSession>,
                val muscleVolume: Map<com.example.heavyduty.core.domain.model.MuscleGroup, Float>,
            )

            combine(
                workoutRepository.getSessionsByDateRange(startOfWeek, now),
                workoutRepository.getCompletedSetsByDateRange(startOfWeek, now),
                programRepository.getActiveProgram(),
                workoutRepository.getAllSessions(),
                getWeeklyMuscleVolume(startOfWeek, now),
            ) { weeklySessions, weeklySets, activeProgram, allSessions, muscleVolume ->
                DashboardData(weeklySessions, weeklySets, activeProgram, allSessions, muscleVolume)
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { data ->
                    val weeklyWorkouts = data.weeklySessions.filter { it.endTime != null }
                    val weeklyCount = weeklyWorkouts.size
                    val totalTime = weeklyWorkouts.sumOf {
                        if (it.endTime != null) ((it.endTime!! - it.startTime) / 1000).toInt() else 0
                    }
                    val weeklyVolume = data.weeklySets
                        .sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }
                        .toFloat()

                    val recentWorkout = data.allSessions
                        .filter { it.endTime != null }
                        .maxByOrNull { it.endTime!! }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weeklyWorkoutsCount = weeklyCount,
                            totalTimeSeconds = totalTime,
                            weeklyVolumeKg = weeklyVolume,
                            weeklyMuscleVolume = data.muscleVolume,
                            recentWorkoutName = recentWorkout?.sessionName?.takeIf { it.isNotBlank() } ?: "Entrenamiento",
                            activeProgramName = data.activeProgram?.name,
                            recentWorkoutTime = recentWorkout?.endTime?.let { time ->
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
