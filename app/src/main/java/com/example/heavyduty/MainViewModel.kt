package com.example.heavyduty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide settings the whole UI reads (see core.ui.LocalWeightUnit). */
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val preferredWeightUnit: StateFlow<WeightUnit> = userPreferencesRepository.preferredWeightUnit
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)
}
