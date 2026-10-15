package com.example.heavyduty.feature.profile.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Off / On for the daily training reminder, and its time. It only turns on once the app may
 * post notifications: asked for where Android requires it (13+), and if they're refused or
 * switched off in the system settings the card says so and offers the way there — a reminder
 * left "on" that can't notify reads as working and never arrives.
 */
@Composable
internal fun TrainingReminderCard(
    reminder: ReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(context.canPostNotifications()) }
    // Settings may have changed while the app was away (this card links there).
    LifecycleResumeEffect(Unit) {
        canNotify = context.canPostNotifications()
        onPauseOrDispose { }
    }
    var refused by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        refused = !granted
        if (granted) onEnabledChange(true)
    }
    var pickingTime by rememberSaveable { mutableStateOf(false) }

    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_reminder),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.profile_reminder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReminderToggle(
            enabled = reminder.enabled,
            onOff = {
                refused = false
                onEnabledChange(false)
            },
            onOn = {
                when {
                    canNotify -> onEnabledChange(true)

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

                    // Before 13 there's nothing to ask: they were switched off in settings.
                    else -> refused = true
                }
            }
        )
        if (!canNotify && (reminder.enabled || refused)) {
            NotificationsBlockedNotice(onOpenSettings = { context.openNotificationSettings() })
        }
        if (reminder.enabled) {
            ReminderTimeRow(hour = reminder.hour, minute = reminder.minute, onChange = { pickingTime = true })
        }
    }

    if (pickingTime) {
        ReminderTimeDialog(
            reminder = reminder,
            onConfirm = { hour, minute ->
                pickingTime = false
                onTimeChange(hour, minute)
            },
            onDismiss = { pickingTime = false }
        )
    }
}

@Composable
private fun ReminderToggle(enabled: Boolean, onOff: () -> Unit, onOn: () -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        HdChip(text = stringResource(R.string.profile_off), isSelected = !enabled, onClick = onOff)
        HdChip(text = stringResource(R.string.profile_on), isSelected = enabled, onClick = onOn)
    }
}

@Composable
private fun ReminderTimeRow(hour: Int, minute: Int, onChange: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.profile_reminder_time, formatTime(hour, minute)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onChange) { Text(stringResource(R.string.profile_reminder_change_time)) }
    }
}

/** Why the reminder can't reach the lifter, and the way to the settings that fix it. */
@Composable
private fun NotificationsBlockedNotice(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.profile_reminder_blocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.profile_reminder_open_settings)) }
    }
}

/** Covers every Android version: the runtime permission (13+) and notifications switched off in settings. */
private fun Context.canPostNotifications(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun formatTime(hour: Int, minute: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(reminder: ReminderSettings, onConfirm: (hour: Int, minute: Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = reminder.hour,
        initialMinute = reminder.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_reminder_time_title)) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
