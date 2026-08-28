package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEffortTrackingEnabledUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = userPreferencesRepository.effortTrackingEnabled
}
