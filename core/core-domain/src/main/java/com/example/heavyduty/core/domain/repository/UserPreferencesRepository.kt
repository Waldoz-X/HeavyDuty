package com.example.heavyduty.core.domain.repository

import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.WeightUnit
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

    /** Whether the active workout screen shows a perceived-effort input. Off by default. */
    val effortTrackingEnabled: Flow<Boolean>
    suspend fun setEffortTrackingEnabled(enabled: Boolean)

    /** "RIR" or "RPE" — which scale new sets are logged in. Defaults to "RIR". */
    val preferredEffortScale: Flow<String>
    suspend fun setPreferredEffortScale(scale: String)

    /**
     * Unit for totals and summaries (volume, body weight, records). Individual sets keep the
     * unit of the machine they were logged on; totals mixing kg and lb equipment need one.
     */
    val preferredWeightUnit: Flow<WeightUnit>
    suspend fun setPreferredWeightUnit(unit: WeightUnit)

    /** The daily training reminder; see [ReminderSettings]. */
    val trainingReminder: Flow<ReminderSettings>
    suspend fun setTrainingReminder(settings: ReminderSettings)
}
