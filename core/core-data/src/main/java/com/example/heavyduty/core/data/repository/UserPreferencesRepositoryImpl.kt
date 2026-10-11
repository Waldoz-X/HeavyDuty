package com.example.heavyduty.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.heavyduty.core.domain.model.DEFAULT_REMINDER_HOUR
import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : UserPreferencesRepository {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val BODY_DIAGRAM_GENDER = stringPreferencesKey("body_diagram_gender")
        val GOAL_WEIGHT_KG = floatPreferencesKey("goal_weight_kg")
        val EFFORT_TRACKING_ENABLED = booleanPreferencesKey("effort_tracking_enabled")
        val PREFERRED_EFFORT_SCALE = stringPreferencesKey("preferred_effort_scale")
        val PREFERRED_WEIGHT_UNIT = stringPreferencesKey("preferred_weight_unit")
        val REMINDER_ENABLED = booleanPreferencesKey("training_reminder_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("training_reminder_minute_of_day")
    }

    private val safeData: Flow<Preferences> = context.userPreferencesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    override val displayName: Flow<String> = safeData
        .map { prefs -> prefs[Keys.DISPLAY_NAME] ?: "" }

    override suspend fun setDisplayName(name: String) {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.DISPLAY_NAME] = name }
    }

    override val bodyDiagramGender: Flow<String> = safeData
        .map { prefs -> prefs[Keys.BODY_DIAGRAM_GENDER] ?: "male" }

    override suspend fun setBodyDiagramGender(gender: String) {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.BODY_DIAGRAM_GENDER] = gender }
    }

    override val goalWeightKg: Flow<Float?> = safeData
        .map { prefs -> prefs[Keys.GOAL_WEIGHT_KG] }

    override suspend fun setGoalWeightKg(kg: Float?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (kg == null) {
                prefs.remove(Keys.GOAL_WEIGHT_KG)
            } else {
                prefs[Keys.GOAL_WEIGHT_KG] = kg
            }
        }
    }

    override val effortTrackingEnabled: Flow<Boolean> = safeData
        .map { prefs -> prefs[Keys.EFFORT_TRACKING_ENABLED] ?: false }

    override suspend fun setEffortTrackingEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.EFFORT_TRACKING_ENABLED] = enabled }
    }

    override val preferredEffortScale: Flow<String> = safeData
        .map { prefs -> prefs[Keys.PREFERRED_EFFORT_SCALE] ?: "RIR" }

    override suspend fun setPreferredEffortScale(scale: String) {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.PREFERRED_EFFORT_SCALE] = scale }
    }

    override val preferredWeightUnit: Flow<WeightUnit> = safeData
        .map { prefs ->
            prefs[Keys.PREFERRED_WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.KG
        }

    override suspend fun setPreferredWeightUnit(unit: WeightUnit) {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.PREFERRED_WEIGHT_UNIT] = unit.name }
    }

    override val trainingReminder: Flow<ReminderSettings> = safeData.map { prefs ->
        val minuteOfDay = prefs[Keys.REMINDER_MINUTE_OF_DAY] ?: (DEFAULT_REMINDER_HOUR * MINUTES_PER_HOUR)
        ReminderSettings(
            enabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            hour = minuteOfDay / MINUTES_PER_HOUR,
            minute = minuteOfDay % MINUTES_PER_HOUR
        )
    }

    override suspend fun setTrainingReminder(settings: ReminderSettings) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = settings.enabled
            prefs[Keys.REMINDER_MINUTE_OF_DAY] = settings.hour * MINUTES_PER_HOUR + settings.minute
        }
    }

    override val onboardingCompleted: Flow<Boolean> = safeData.map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted() {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = true }
    }
}

private const val MINUTES_PER_HOUR = 60
