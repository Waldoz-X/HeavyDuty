package com.example.heavyduty.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user-facing preferences (profile display name, etc.).
 */
interface UserPreferencesRepository {
    val displayName: Flow<String>
    suspend fun setDisplayName(name: String)
}
