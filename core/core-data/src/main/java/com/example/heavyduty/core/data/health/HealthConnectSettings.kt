package com.example.heavyduty.core.data.health

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The link's own state: whether it's on, and up to when weigh-ins were already brought in. */
internal interface HealthConnectSettings {
    val enabled: Flow<Boolean>
    suspend fun setEnabled(enabled: Boolean)
    val weightsImportedUntil: Flow<Long?>
    suspend fun setWeightsImportedUntil(millis: Long)
}

private val Context.healthConnectDataStore by preferencesDataStore(name = "health_connect")

@Singleton
internal class DataStoreHealthConnectSettings @Inject constructor(@param:ApplicationContext private val context: Context) :
    HealthConnectSettings {

    override val enabled: Flow<Boolean> = context.healthConnectDataStore.data.map { it[ENABLED] ?: false }

    override suspend fun setEnabled(enabled: Boolean) {
        context.healthConnectDataStore.edit { it[ENABLED] = enabled }
    }

    override val weightsImportedUntil: Flow<Long?> = context.healthConnectDataStore.data.map { it[WEIGHTS_IMPORTED_UNTIL] }

    override suspend fun setWeightsImportedUntil(millis: Long) {
        context.healthConnectDataStore.edit { it[WEIGHTS_IMPORTED_UNTIL] = millis }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val WEIGHTS_IMPORTED_UNTIL = longPreferencesKey("weights_imported_until")
    }
}
