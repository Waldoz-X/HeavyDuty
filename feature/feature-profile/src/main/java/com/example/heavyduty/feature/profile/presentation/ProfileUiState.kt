package com.example.heavyduty.feature.profile.presentation

import android.net.Uri
import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.ThemeMode
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.feature.profile.domain.model.ProfileStats

/**
 * Immutable UI state for the profile screen.
 */
data class ProfileUiState(
    val displayName: String = "",
    val nameInput: String = "",
    val isEditingName: Boolean = false,
    val stats: ProfileStats = ProfileStats(),
    val feedItems: List<com.example.heavyduty.core.ui.components.WorkoutFeedItem> = emptyList(),
    val weeklyHoursChartData: List<Pair<String, Float>> = emptyList(),
    val currentStreakDays: Int = 0,
    val bodyDiagramGender: String = "male",
    val effortTrackingEnabled: Boolean = false,
    val preferredEffortScale: String = "RIR",
    val preferredWeightUnit: WeightUnit = WeightUnit.KG,
    val reminder: ReminderSettings = ReminderSettings(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isLoading: Boolean = true,
    /** The data couldn't be read; the screen shows an error instead of looking empty. */
    val loadFailed: Boolean = false,
    val isExportingData: Boolean = false,
    val isImportingData: Boolean = false,
    val backupMessage: String? = null
)

/**
 * User-initiated events from the profile screen.
 */
sealed interface ProfileUiEvent {
    data object OnStartEditingName : ProfileUiEvent
    data class OnNameInputChange(val name: String) : ProfileUiEvent
    data object OnSaveName : ProfileUiEvent
    data object OnCancelEditingName : ProfileUiEvent
    data class OnBodyDiagramGenderChange(val gender: String) : ProfileUiEvent
    data class OnEffortTrackingEnabledChange(val enabled: Boolean) : ProfileUiEvent
    data class OnPreferredEffortScaleChange(val scale: String) : ProfileUiEvent
    data class OnReminderEnabledChange(val enabled: Boolean) : ProfileUiEvent
    data class OnThemeModeChange(val mode: ThemeMode) : ProfileUiEvent
    data class OnReminderTimeChange(val hour: Int, val minute: Int) : ProfileUiEvent
    data class OnPreferredWeightUnitChange(val unit: WeightUnit) : ProfileUiEvent
    data class OnExportDataRequested(val uri: Uri) : ProfileUiEvent
    data class OnImportDataRequested(val uri: Uri) : ProfileUiEvent
    data object OnDismissBackupMessage : ProfileUiEvent
}
