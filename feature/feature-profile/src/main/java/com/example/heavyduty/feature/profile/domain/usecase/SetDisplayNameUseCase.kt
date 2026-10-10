package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetDisplayNameUseCase @Inject constructor(private val userPreferencesRepository: UserPreferencesRepository) {
    suspend operator fun invoke(name: String) {
        userPreferencesRepository.setDisplayName(name.trim())
    }
}
