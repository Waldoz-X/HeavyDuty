package com.example.heavyduty.feature.profile.domain.usecase

import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPreferredEffortScaleUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<String> = userPreferencesRepository.preferredEffortScale
}
