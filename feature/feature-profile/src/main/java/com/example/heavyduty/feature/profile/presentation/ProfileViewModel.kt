package com.example.heavyduty.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.profile.domain.usecase.GetDisplayNameUseCase
import com.example.heavyduty.feature.profile.domain.usecase.GetProfileStatsUseCase
import com.example.heavyduty.feature.profile.domain.usecase.SetDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.ui.components.WorkoutFeedItem
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getDisplayNameUseCase: GetDisplayNameUseCase,
    private val setDisplayNameUseCase: SetDisplayNameUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getDisplayNameUseCase(),
                getProfileStatsUseCase(),
                workoutRepository.getAllSessions(),
                workoutRepository.getAllCompletedSets(),
                exerciseRepository.getAllExercises()
            ) { name, stats, sessions, sets, exercises ->
                val exerciseMap = exercises.associateBy { it.id }

                // A set counts as a PR the first time it beats the heaviest weight ever
                // lifted for that exercise, walking through history chronologically.
                val personalRecordSetIds = run {
                    val bestWeightByExercise = mutableMapOf<Long, Float>()
                    sets
                        .filter { it.weight != null && it.completedAt != null }
                        .sortedBy { it.completedAt }
                        .mapNotNull { set ->
                            val weight = set.weight ?: return@mapNotNull null
                            val currentBest = bestWeightByExercise[set.exerciseId] ?: 0f
                            if (weight > currentBest) {
                                bestWeightByExercise[set.exerciseId] = weight
                                set.id
                            } else {
                                null
                            }
                        }
                        .toSet()
                }

                // Process Feed Items
                val sortedSessions = sessions.sortedByDescending { it.startTime }
                val feedItems = sortedSessions.map { session ->
                    val sessionSets = sets.filter { it.sessionId == session.id }
                    val volume = sessionSets.sumOf { (it.weight ?: 0f) * (it.reps ?: 0) }.toFloat()
                    val prs = sessionSets.count { it.id in personalRecordSetIds }

                    // Group sets by exercise to create the string "3 series Dominada"
                    val exerciseCounts = sessionSets.groupBy { it.exerciseId }
                    val exerciseStrings = exerciseCounts.mapNotNull { (exId, exSets) ->
                        exerciseMap[exId]?.let { ex -> "${exSets.size} series ${ex.name}" }
                    }

                    WorkoutFeedItem(
                        id = session.id,
                        userName = name.ifBlank { "Atleta" },
                        userAvatarUrl = null,
                        relativeTime = formatRelativeTime(session.startTime),
                        workoutName = session.sessionName.ifBlank { "Entrenamiento" },
                        durationMinutes = session.totalDurationSecs / 60,
                        volumeKg = volume,
                        personalRecords = prs,
                        exercises = exerciseStrings
                    )
                }

                // Process Chart Data (Last 4 weeks hours)
                val chartData = mutableListOf<Pair<String, Float>>()
                val cal = Calendar.getInstance()
                val format = SimpleDateFormat("dd MMM", Locale("es", "ES"))
                
                for (i in 3 downTo 0) {
                    val end = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    val start = cal.timeInMillis
                    
                    val weekSessions = sortedSessions.filter { it.startTime in start..end }
                    val weekHours = weekSessions.sumOf { it.totalDurationSecs } / 3600f
                    chartData.add(Pair(format.format(cal.time), weekHours))
                }

                _uiState.update {
                    it.copy(
                        displayName = name,
                        stats = stats,
                        feedItems = feedItems,
                        weeklyHoursChartData = chartData,
                        currentStreakDays = calculateCurrentStreakDays(sessions),
                        isLoading = false,
                    )
                }
            }.collect { }
        }
    }

    /**
     * Consecutive days (ending today) with at least one completed workout.
     * A missed "today" doesn't break a streak that was active as of yesterday.
     */
    private fun calculateCurrentStreakDays(sessions: List<com.example.heavyduty.core.domain.model.WorkoutSession>): Int {
        val trainedDays = sessions
            .filter { it.endTime != null }
            .map { dayStartMillis(it.startTime) }
            .toSet()
        if (trainedDays.isEmpty()) return 0

        val cursor = Calendar.getInstance()
        if (!trainedDays.contains(dayStartMillis(cursor.timeInMillis))) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        while (trainedDays.contains(dayStartMillis(cursor.timeInMillis))) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun dayStartMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        return when {
            days > 0 -> "hace $days días"
            hours > 0 -> "hace $hours horas"
            else -> "hace un momento"
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.OnStartEditingName -> {
                _uiState.update { it.copy(isEditingName = true, nameInput = it.displayName) }
            }
            is ProfileUiEvent.OnNameInputChange -> {
                _uiState.update { it.copy(nameInput = event.name) }
            }
            ProfileUiEvent.OnCancelEditingName -> {
                _uiState.update { it.copy(isEditingName = false, nameInput = "") }
            }
            ProfileUiEvent.OnSaveName -> saveName()
        }
    }

    private fun saveName() {
        val name = _uiState.value.nameInput
        viewModelScope.launch {
            setDisplayNameUseCase(name)
            _uiState.update { it.copy(isEditingName = false) }
        }
    }
}
