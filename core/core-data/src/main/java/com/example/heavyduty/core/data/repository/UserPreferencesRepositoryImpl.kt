package com.example.heavyduty.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val BODY_DIAGRAM_GENDER = stringPreferencesKey("body_diagram_gender")
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
}
