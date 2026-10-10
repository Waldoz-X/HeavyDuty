package com.example.heavyduty.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.heavyduty.ForgottenWorkout
import com.example.heavyduty.core.common.R

/**
 * "Still training?" for a workout left open with nothing logged for hours: finish it (saved up
 * to its last set), discard it, or carry on. Dismissing it counts as carrying on.
 */
@Composable
fun ForgottenWorkoutDialog(workout: ForgottenWorkout, onFinish: () -> Unit, onDiscard: () -> Unit, onKeepTraining: () -> Unit) {
    val name = workout.sessionName.ifBlank { stringResource(R.string.workout_default_name) }
    val hint = if (workout.setsLogged > 0) R.string.forgotten_workout_finish_hint else R.string.forgotten_workout_empty_hint
    AlertDialog(
        onDismissRequest = onKeepTraining,
        title = { Text(stringResource(R.string.forgotten_workout_title)) },
        text = {
            Text(
                pluralStringResource(R.plurals.forgotten_workout_message, workout.idleHours, name, workout.idleHours) +
                    "\n\n" + stringResource(hint)
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.workout_discard_confirm), color = MaterialTheme.colorScheme.error)
                }
                // With nothing logged there's nothing worth saving: only discarding makes sense.
                if (workout.setsLogged > 0) {
                    TextButton(onClick = onFinish) { Text(stringResource(R.string.forgotten_workout_finish)) }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepTraining) { Text(stringResource(R.string.forgotten_workout_keep)) }
        }
    )
}
