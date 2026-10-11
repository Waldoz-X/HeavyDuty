package com.example.heavyduty.feature.profile.presentation.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.HealthSyncResult
import com.example.heavyduty.core.domain.repository.HealthSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthConnectUiState(
    val status: HealthConnectStatus = HealthConnectStatus.UNSUPPORTED,
    val connected: Boolean = false,
    val isSyncing: Boolean = false,
    val message: HealthConnectMessage? = null
)

/** The outcome of the last thing the user did, shown under the card's buttons. */
sealed interface HealthConnectMessage {
    data class Synced(val result: HealthSyncResult) : HealthConnectMessage
    data object Failed : HealthConnectMessage
    data object PermissionsDenied : HealthConnectMessage
}

/** Perfil's Health Connect card: connect, sync now, disconnect. */
@HiltViewModel
class HealthConnectViewModel @Inject constructor(private val healthSync: HealthSync) : ViewModel() {

    /** What to ask Health Connect's permission screen for. */
    val permissions: Set<String> = healthSync.permissions

    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    /** Called whenever Perfil comes back: Health Connect may have been installed, or permissions changed, meanwhile. */
    fun refresh() {
        viewModelScope.launch {
            val status = healthSync.status()
            val connected =
                status == HealthConnectStatus.AVAILABLE && healthSync.enabled.first() && healthSync.grantedPermissions().isNotEmpty()
            _uiState.update { it.copy(status = status, connected = connected) }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        if (granted.isEmpty()) {
            _uiState.update { it.copy(message = HealthConnectMessage.PermissionsDenied) }
        } else {
            sync { healthSync.connect() }
        }
    }

    fun syncNow() = sync { healthSync.syncNow() }

    fun disconnect() {
        viewModelScope.launch {
            healthSync.disconnect()
            _uiState.update { it.copy(connected = false, message = null) }
        }
    }

    private fun sync(block: suspend () -> HealthSyncResult) {
        _uiState.update { it.copy(isSyncing = true, message = null) }
        viewModelScope.launch {
            try {
                val result = block()
                _uiState.update { it.copy(connected = true, isSyncing = false, message = HealthConnectMessage.Synced(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isSyncing = false, message = HealthConnectMessage.Failed) }
            }
        }
    }
}
