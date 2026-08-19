package com.example.heavyduty.feature.profile.presentation

import com.example.heavyduty.feature.profile.domain.model.ProfileStats

/**
 * Immutable UI state for the profile screen.
 */
data class ProfileUiState(
    val displayName: String = "",
    val isEditingName: Boolean = false,
    val nameInput: String = "",
    val stats: ProfileStats = ProfileStats(),
    val isLoading: Boolean = true,
)

/**
 * User-initiated events from the profile screen.
 */
sealed interface ProfileUiEvent {
    data object OnStartEditingName : ProfileUiEvent
    data class OnNameInputChange(val name: String) : ProfileUiEvent
    data object OnSaveName : ProfileUiEvent
    data object OnCancelEditingName : ProfileUiEvent
}
