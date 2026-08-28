package com.example.heavyduty.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user-facing preferences (profile display name, etc.).
 */
interface UserPreferencesRepository {
    val displayName: Flow<String>
    suspend fun setDisplayName(name: String)

    /** "male" or "female" — which body figure the muscle-map diagrams draw. Defaults to "male". */
    val bodyDiagramGender: Flow<String>
    suspend fun setBodyDiagramGender(gender: String)

    /** Target body weight in kg for the Dashboard's weight chart goal line. Null = no goal set. */
    val goalWeightKg: Flow<Float?>
    suspend fun setGoalWeightKg(kg: Float?)
}
