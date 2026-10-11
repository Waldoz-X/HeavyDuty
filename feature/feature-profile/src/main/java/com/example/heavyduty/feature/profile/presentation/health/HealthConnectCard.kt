package com.example.heavyduty.feature.profile.presentation.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdTextButton

/** Health Connect in Perfil: install it, connect, or — once connected — sync, manage, disconnect. */
@Composable
internal fun HealthConnectCard(
    uiState: HealthConnectUiState,
    onInstall: () -> Unit,
    onConnect: () -> Unit,
    onSyncNow: () -> Unit,
    onManagePermissions: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.profile_health_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (uiState.connected) ConnectedBadge()
        }
        val needsInstall = uiState.status == HealthConnectStatus.NEEDS_INSTALL
        Text(
            text = stringResource(if (needsInstall) R.string.profile_health_install_hint else R.string.profile_health_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            needsInstall -> HdButton(text = stringResource(R.string.profile_health_install), onClick = onInstall)

            !uiState.connected -> HdButton(
                text = stringResource(R.string.profile_health_connect),
                onClick = onConnect,
                isLoading = uiState.isSyncing
            )

            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HdButton(
                    text = stringResource(R.string.profile_health_sync_now),
                    onClick = onSyncNow,
                    isLoading = uiState.isSyncing,
                    enabled = !uiState.isSyncing,
                    icon = Icons.Default.Sync
                )
                HdTextButton(text = stringResource(R.string.profile_health_permissions), onClick = onManagePermissions)
                HdTextButton(
                    text = stringResource(R.string.profile_health_disconnect),
                    onClick = onDisconnect,
                    enabled = !uiState.isSyncing
                )
            }
        }
        uiState.message?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            val isError = message !is HealthConnectMessage.Synced
            Text(
                text = messageText(message),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConnectedBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.profile_health_connected),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun messageText(message: HealthConnectMessage): String = when (message) {
    is HealthConnectMessage.Synced -> {
        val (workouts, weights) = message.result
        listOf(
            pluralStringResource(R.plurals.profile_health_workouts_sent, workouts, workouts),
            pluralStringResource(R.plurals.profile_health_weights_imported, weights, weights)
        ).joinToString(" · ")
    }

    HealthConnectMessage.Failed -> stringResource(R.string.profile_health_failed)

    HealthConnectMessage.PermissionsDenied -> stringResource(R.string.profile_health_denied)
}
