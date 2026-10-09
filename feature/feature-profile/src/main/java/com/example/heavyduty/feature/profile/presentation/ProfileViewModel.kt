package com.example.heavyduty.feature.profile.presentation

import com.example.heavyduty.core.domain.model.displayName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.profile.domain.model.ProfileStats
import com.example.heavyduty.feature.profile.domain.model.personalRecordSetIds
import com.example.heavyduty.feature.profile.domain.model.weeklyTrainingHours
import com.example.heavyduty.core.domain.model.currentStreakDays
import com.example.heavyduty.feature.profile.domain.usecase.ExportBackupUseCase
import com.example.heavyduty.feature.profile.domain.usecase.GetBodyDiagramGenderUseCase
import com.example.heavyduty.feature.profile.domain.usecase.GetDisplayNameUseCase
import com.example.heavyduty.feature.profile.domain.usecase.GetEffortTrackingEnabledUseCase
import com.example.heavyduty.feature.profile.domain.usecase.GetPreferredEffortScaleUseCase
import com.example.heavyduty.feature.profile.domain.usecase.GetProfileStatsUseCase
import com.example.heavyduty.feature.profile.domain.usecase.ImportBackupUseCase
import com.example.heavyduty.feature.profile.domain.usecase.SetBodyDiagramGenderUseCase
import com.example.heavyduty.feature.profile.domain.usecase.SetDisplayNameUseCase
import com.example.heavyduty.feature.profile.domain.usecase.SetEffortTrackingEnabledUseCase
import com.example.heavyduty.feature.profile.domain.usecase.SetPreferredEffortScaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.ui.components.WorkoutFeedItem
import com.example.heavyduty.core.common.R
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class ProfileViewModel @Inject constructor(
    getDisplayNameUseCase: GetDisplayNameUseCase,
    private val setDisplayNameUseCase: SetDisplayNameUseCase,
    getProfileStatsUseCase: GetProfileStatsUseCase,
    getBodyDiagramGenderUseCase: GetBodyDiagramGenderUseCase,
    private val setBodyDiagramGenderUseCase: SetBodyDiagramGenderUseCase,
    getEffortTrackingEnabledUseCase: GetEffortTrackingEnabledUseCase,
    private val setEffortTrackingEnabledUseCase: SetEffortTrackingEnabledUseCase,
    getPreferredEffortScaleUseCase: GetPreferredEffortScaleUseCase,
    private val setPreferredEffortScaleUseCase: SetPreferredEffortScaleUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val profileDataFlow = combine(
            getDisplayNameUseCase(),
            getProfileStatsUseCase(),
            workoutRepository.getAllSessions(),
            workoutRepository.getAllCompletedSets(),
            exerciseRepository.getAllExercises()
        ) { name, stats, sessions, rawSets, exercises ->
            val sets = rawSets.excludeWarmups()
            val exerciseMap = exercises.associateBy { it.id }

            // A set counts as a PR the first time it beats the heaviest weight ever
            // lifted for that exercise, walking through history chronologically.
            val personalRecordSetIds = personalRecordSetIds(sets)

            // Group sets by session for O(N) lookup
            val setsBySession = sets.groupBy { it.sessionId }

            // Process Feed Items
            val sortedSessions = sessions.sortedByDescending { it.startTime }
            val feedItems = sortedSessions.map { session ->
                val sessionSets = setsBySession[session.id] ?: emptyList()
                val volume = sessionSets.map { (it.weight ?: 0f) * (it.reps ?: 0) }.sum()
                val prs = sessionSets.count { it.id in personalRecordSetIds }

                // Group sets by exercise to create the string "3 series Dominada"
                val exerciseCounts = sessionSets.groupBy { it.exerciseId }
                val setsStr = appContext.getString(R.string.sets_lowercase)
                val exerciseStrings = exerciseCounts.mapNotNull { (exId, exSets) ->
                    exerciseMap[exId]?.let { ex -> "${exSets.size} $setsStr ${ex.displayName}" }
                }

                WorkoutFeedItem(
                    id = session.id,
                    userName = name.ifBlank { appContext.getString(R.string.athlete) },
                    userAvatarUrl = null,
                    relativeTime = formatRelativeTime(session.startTime),
                    workoutName = session.sessionName.ifBlank { appContext.getString(R.string.training) },
                    durationMinutes = session.totalDurationSecs / 60,
                    volumeKg = volume,
                    personalRecords = prs,
                    exercises = exerciseStrings
                )
            }

            // Hours per week over the last 4 weeks, oldest first, labeled by each week's start.
            val format = SimpleDateFormat("dd MMM", Locale.getDefault())
            val chartData = weeklyTrainingHours(sessions, now = System.currentTimeMillis())
                .map { (weekStart, hours) -> format.format(java.util.Date(weekStart)) to hours }

            ProfileFlowData(
                name = name,
                stats = stats,
                feedItems = feedItems,
                chartData = chartData,
                streak = currentStreakDays(sessions),
            )
        }.flowOn(Dispatchers.Default)

        val settingsFlow = combine(
            getBodyDiagramGenderUseCase(),
            getEffortTrackingEnabledUseCase(),
            getPreferredEffortScaleUseCase(),
            userPreferencesRepository.preferredWeightUnit,
        ) { gender, effortEnabled, effortScale, weightUnit ->
            ProfileSettings(gender, effortEnabled, effortScale, weightUnit)
        }

        viewModelScope.launch {
            combine(profileDataFlow, settingsFlow) { data, settings ->
                _uiState.update {
                    it.copy(
                        displayName = data.name,
                        stats = data.stats,
                        feedItems = data.feedItems,
                        weeklyHoursChartData = data.chartData,
                        currentStreakDays = data.streak,
                        bodyDiagramGender = settings.bodyDiagramGender,
                        effortTrackingEnabled = settings.effortTrackingEnabled,
                        preferredEffortScale = settings.preferredEffortScale,
                        preferredWeightUnit = settings.preferredWeightUnit,
                        isLoading = false,
                    )
                }
            }.collect { }
        }
    }

    private data class ProfileFlowData(
        val name: String,
        val stats: ProfileStats,
        val feedItems: List<WorkoutFeedItem>,
        val chartData: List<Pair<String, Float>>,
        val streak: Int,
    )

    private data class ProfileSettings(
        val bodyDiagramGender: String,
        val effortTrackingEnabled: Boolean,
        val preferredEffortScale: String,
        val preferredWeightUnit: WeightUnit,
    )

    private fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        return when {
            days > 0 -> appContext.resources.getQuantityString(R.plurals.time_ago_days, days.toInt(), days)
            hours > 0 -> appContext.resources.getQuantityString(R.plurals.time_ago_hours, hours.toInt(), hours)
            else -> appContext.getString(R.string.time_ago_just_now)
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
            is ProfileUiEvent.OnBodyDiagramGenderChange -> {
                viewModelScope.launch { setBodyDiagramGenderUseCase(event.gender) }
            }
            is ProfileUiEvent.OnEffortTrackingEnabledChange -> {
                viewModelScope.launch { setEffortTrackingEnabledUseCase(event.enabled) }
            }
            is ProfileUiEvent.OnPreferredEffortScaleChange -> {
                viewModelScope.launch { setPreferredEffortScaleUseCase(event.scale) }
            }
            is ProfileUiEvent.OnPreferredWeightUnitChange -> {
                viewModelScope.launch { userPreferencesRepository.setPreferredWeightUnit(event.unit) }
            }
            is ProfileUiEvent.OnExportDataRequested -> exportData(event.uri)
            is ProfileUiEvent.OnImportDataRequested -> importData(event.uri)
            ProfileUiEvent.OnDismissBackupMessage -> {
                _uiState.update { it.copy(backupMessage = null) }
            }
        }
    }

    private fun exportData(uri: Uri) {
        _uiState.update { it.copy(isExportingData = true) }
        viewModelScope.launch {
            try {
                val json = exportBackupUseCase()
                appContext.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray())
                } ?: throw IllegalStateException(appContext.getString(R.string.backup_export_error_open))
                _uiState.update { it.copy(isExportingData = false, backupMessage = appContext.getString(R.string.backup_exported)) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isExportingData = false, backupMessage = appContext.getString(R.string.backup_export_error)) }
            }
        }
    }

    private fun importData(uri: Uri) {
        _uiState.update { it.copy(isImportingData = true) }
        viewModelScope.launch {
            try {
                val json = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException(appContext.getString(R.string.backup_import_error_read))
                val result = importBackupUseCase(json)
                val message = buildString {
                    append(appContext.getString(R.string.backup_imported_stats, result.routinesImported, result.sessionsImported, result.bodyWeightEntriesImported, result.customExercisesImported))
                    if (result.skippedReferences > 0) {
                        append(appContext.resources.getQuantityString(R.plurals.backup_imported_skipped, result.skippedReferences, result.skippedReferences))
                    }
                }
                _uiState.update { it.copy(isImportingData = false, backupMessage = message) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isImportingData = false, backupMessage = appContext.getString(R.string.backup_import_error_invalid)) }
            }
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
