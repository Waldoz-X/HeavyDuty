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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getDisplayNameUseCase: GetDisplayNameUseCase,
    private val setDisplayNameUseCase: SetDisplayNameUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getDisplayNameUseCase(),
                getProfileStatsUseCase(),
            ) { name, stats -> name to stats }
                .collect { (name, stats) ->
                    _uiState.update {
                        it.copy(
                            displayName = name,
                            stats = stats,
                            isLoading = false,
                        )
                    }
                }
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
