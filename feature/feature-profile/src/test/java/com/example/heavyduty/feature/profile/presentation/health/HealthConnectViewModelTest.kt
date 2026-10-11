package com.example.heavyduty.feature.profile.presentation.health

import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.domain.model.HealthSyncResult
import com.example.heavyduty.core.domain.repository.HealthSync
import com.example.heavyduty.core.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthConnectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val healthSync = FakeHealthSync()
    private val viewModel = HealthConnectViewModel(healthSync)

    @Test
    fun `on a phone with Health Connect, the card offers to connect`() = runTest {
        viewModel.refresh()

        assertEquals(HealthConnectStatus.AVAILABLE, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.connected)
    }

    @Test
    fun `granting permissions connects and shows what was synced`() = runTest {
        viewModel.onPermissionsResult(healthSync.permissions)

        val state = viewModel.uiState.value
        assertTrue(state.connected)
        assertFalse(state.isSyncing)
        assertEquals(HealthConnectMessage.Synced(HealthSyncResult(workoutsExported = 3, weightsImported = 1)), state.message)
    }

    @Test
    fun `denying every permission says so and doesn't connect`() = runTest {
        viewModel.onPermissionsResult(emptySet())

        assertEquals(HealthConnectMessage.PermissionsDenied, viewModel.uiState.value.message)
        assertFalse(healthSync.enabled.value)
    }

    @Test
    fun `a failed sync is reported, not thrown`() = runTest {
        healthSync.failing = true

        viewModel.syncNow()

        assertEquals(HealthConnectMessage.Failed, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isSyncing)
    }

    @Test
    fun `disconnecting turns the link off`() = runTest {
        viewModel.onPermissionsResult(healthSync.permissions)

        viewModel.disconnect()

        assertFalse(viewModel.uiState.value.connected)
        assertFalse(healthSync.enabled.value)
    }

    @Test
    fun `permissions revoked in Health Connect show as disconnected on return`() = runTest {
        viewModel.onPermissionsResult(healthSync.permissions)
        healthSync.granted = emptySet()

        viewModel.refresh()

        assertFalse(viewModel.uiState.value.connected)
    }
}

private class FakeHealthSync : HealthSync {
    override val permissions = setOf("write", "read")
    override val enabled = MutableStateFlow(false)
    var granted: Set<String> = permissions
    var failing = false

    override fun status() = HealthConnectStatus.AVAILABLE
    override suspend fun grantedPermissions() = granted

    override suspend fun connect(): HealthSyncResult {
        enabled.value = true
        return syncNow()
    }

    override suspend fun disconnect() {
        enabled.value = false
    }

    override suspend fun syncNow(): HealthSyncResult {
        if (failing) throw IOException("Health Connect is down")
        return HealthSyncResult(workoutsExported = 3, weightsImported = 1)
    }

    override suspend fun importNewWeights() = Unit
    override suspend fun workoutSaved(sessionId: Long) = Unit
    override suspend fun workoutDeleted(sessionId: Long) = Unit
}
