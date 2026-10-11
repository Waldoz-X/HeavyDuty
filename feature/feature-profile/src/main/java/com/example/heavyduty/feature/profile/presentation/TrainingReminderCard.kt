package com.example.heavyduty.feature.profile.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Off / On for the daily training reminder, and its time. Turning it on asks for permission to
 * post notifications where Android requires it; without it the reminder simply doesn't show.
 */
@Composable
internal fun TrainingReminderCard(reminder: ReminderSettings, onEvent: (ProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HdChip(
                text = stringResource(R.string.profile_off),
                isSelected = !reminder.enabled,
                onClick = { onEvent(ProfileUiEvent.OnReminderEnabledChange(false)) }
            )
            HdChip(
                text = stringResource(R.string.profile_on),
                isSelected = reminder.enabled,
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    onEvent(ProfileUiEvent.OnReminderEnabledChange(true))
                }
            )
        }
        if (reminder.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.profile_reminder_time, formatTime(reminder.hour, reminder.minute)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { pickingTime = true }) { Text(stringResource(R.string.profile_reminder_change_time)) }
            }
        }
    }

    if (pickingTime) {
        ReminderTimeDialog(
            reminder = reminder,
            onConfirm = { hour, minute ->
                pickingTime = false
                onEvent(ProfileUiEvent.OnReminderTimeChange(hour, minute))
            },
            onDismiss = { pickingTime = false }
        )
    }
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
