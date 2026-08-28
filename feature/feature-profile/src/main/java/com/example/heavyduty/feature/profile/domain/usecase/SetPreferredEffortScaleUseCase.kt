package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetPreferredEffortScaleUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(scale: String) {
        userPreferencesRepository.setPreferredEffortScale(scale)
    }
}
