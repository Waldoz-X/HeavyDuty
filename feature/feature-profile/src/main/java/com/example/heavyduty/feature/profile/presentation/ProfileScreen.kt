package com.example.heavyduty.feature.profile.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.HealthConnectStatus
import com.example.heavyduty.core.ui.components.HdBarChart
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.WorkoutFeedCard
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.feature.profile.presentation.health.HealthConnectCard
import com.example.heavyduty.feature.profile.presentation.health.HealthConnectViewModel
import com.example.heavyduty.feature.profile.presentation.health.openHealthConnectInStore
import com.example.heavyduty.feature.profile.presentation.health.openHealthConnectPermissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToStats: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    healthViewModel: HealthConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val healthState by healthViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Health Connect's own permission screen; it returns the permissions now granted.
    val healthPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted -> healthViewModel.onPermissionsResult(granted) }
    LifecycleResumeEffect(Unit) {
        healthViewModel.refresh()
        onPauseOrDispose { }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.onEvent(ProfileUiEvent.OnExportDataRequested(it)) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onEvent(ProfileUiEvent.OnImportDataRequested(it)) } }

    when {
        uiState.isLoading -> HdCardsSkeleton(modifier = modifier.padding(16.dp), heights = listOf(120.dp, 160.dp, 140.dp, 140.dp))

        uiState.loadFailed -> HdErrorState(modifier = modifier)

        else -> ProfileContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onNavigateToStats = onNavigateToStats,
            onExportClick = {
                val fileName = "heavyduty_backup_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"
                exportLauncher.launch(fileName)
            },
            onImportClick = { importLauncher.launch(arrayOf("application/json")) },
            healthConnectCard = {
                if (healthState.status != HealthConnectStatus.UNSUPPORTED) {
                    HealthConnectCard(
                        uiState = healthState,
                        onInstall = { context.openHealthConnectInStore() },
                        onConnect = { healthPermissions.launch(healthViewModel.permissions) },
                        onSyncNow = healthViewModel::syncNow,
                        onManagePermissions = { context.openHealthConnectPermissions() },
                        onDisconnect = healthViewModel::disconnect
                    )
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    onNavigateToStats: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    healthConnectCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        item { ProfileHeader(uiState = uiState, onEvent = onEvent) }
        if (uiState.weeklyHoursChartData.isNotEmpty()) {
            item { WeeklyHoursCard(data = uiState.weeklyHoursChartData) }
        }
        item { GlobalStats(uiState = uiState) }
        item { StatsLinkCard(onClick = onNavigateToStats) }
        settingsItems(uiState = uiState, onEvent = onEvent, healthConnectCard = healthConnectCard)
        item {
            YourDataCard(
                isBusy = uiState.isExportingData || uiState.isImportingData,
                message = uiState.backupMessage,
                onExportClick = onExportClick,
                onImportClick = onImportClick,
                onDismissMessage = { onEvent(ProfileUiEvent.OnDismissBackupMessage) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.profile_workouts),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        items(uiState.feedItems) { feedItem -> WorkoutFeedCard(item = feedItem) }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

/** The settings cards, in the order Perfil lists them. */
private fun LazyListScope.settingsItems(
    uiState: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    healthConnectCard: @Composable () -> Unit
) {
    item { BodyFigureSetting(gender = uiState.bodyDiagramGender, onChange = { onEvent(ProfileUiEvent.OnBodyDiagramGenderChange(it)) }) }
    item {
        EffortSetting(
            enabled = uiState.effortTrackingEnabled,
            scale = uiState.preferredEffortScale,
            onEnabledChange = { onEvent(ProfileUiEvent.OnEffortTrackingEnabledChange(it)) },
            onScaleChange = { onEvent(ProfileUiEvent.OnPreferredEffortScaleChange(it)) }
        )
    }
    item { ThemeModeCard(selected = uiState.themeMode, onSelect = { onEvent(ProfileUiEvent.OnThemeModeChange(it)) }) }
    item {
        TrainingReminderCard(
            reminder = uiState.reminder,
            onEnabledChange = { onEvent(ProfileUiEvent.OnReminderEnabledChange(it)) },
            onTimeChange = { hour, minute -> onEvent(ProfileUiEvent.OnReminderTimeChange(hour, minute)) }
        )
    }
    item { healthConnectCard() }
    item { WeightUnitSetting(unit = uiState.preferredWeightUnit, onChange = { onEvent(ProfileUiEvent.OnPreferredWeightUnitChange(it)) }) }
}

@Composable
private fun WeeklyHoursCard(data: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.profile_hours_trained),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            HdBarChart(data = data, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GlobalStats(uiState: ProfileUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.profile_global_stats),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.profile_workouts),
                value = uiState.stats.totalWorkouts.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.MonitorWeight,
                label = stringResource(R.string.metric_volume),
                value = uiState.stats.totalVolumeKg.formatTotalWeight(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.LocalFireDepartment,
                label = stringResource(R.string.profile_current_streak),
                value = stringResource(R.string.profile_streak_days, uiState.currentStreakDays),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatsLinkCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.profile_view_stats),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The initial, the name, and editing it in place. */
@Composable
private fun ProfileHeader(uiState: ProfileUiState, onEvent: (ProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.displayName.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (uiState.isEditingName) {
                NameEditor(name = uiState.nameInput, onEvent = onEvent, modifier = Modifier.weight(1f))
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.displayName.ifBlank { stringResource(R.string.profile_add_name) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.profile_tap_to_edit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onEvent(ProfileUiEvent.OnStartEditingName) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.profile_edit_name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NameEditor(name: String, onEvent: (ProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = { onEvent(ProfileUiEvent.OnNameInputChange(it)) },
            placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            HdButton(text = stringResource(R.string.action_save), onClick = { onEvent(ProfileUiEvent.OnSaveName) })
            Spacer(modifier = Modifier.width(8.dp))
            HdTextButton(text = stringResource(R.string.action_cancel), onClick = { onEvent(ProfileUiEvent.OnCancelEditingName) })
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
