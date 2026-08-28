package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetEffortTrackingEnabledUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        userPreferencesRepository.setEffortTrackingEnabled(enabled)
    }
}
