package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetEffortTrackingEnabledUseCase @Inject constructor(private val userPreferencesRepository: UserPreferencesRepository) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.effortTrackingEnabled
}
