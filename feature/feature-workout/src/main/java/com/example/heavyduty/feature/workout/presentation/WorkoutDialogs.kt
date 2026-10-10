package com.example.heavyduty.feature.workout.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.barPresets
import com.example.heavyduty.core.domain.model.calculatePlatesPerSide
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.model.standardPlates
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdExerciseThumbnail
import com.example.heavyduty.core.ui.components.HdSearchBar
import com.example.heavyduty.core.ui.nameResId

// Dialogs and pickers opened from the workout screen.

@Composable
internal fun FinishWorkoutDialog(uiState: ActiveWorkoutUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val planned = uiState.plannedSetCount
    val done = uiState.completedPlannedSetCount
    val message = if (planned > 0 && done < planned) {
        // Keyed on what's missing: "Te falta 1 serie… Quedará" / "Te faltan 3 series… Quedarán".
        val missing = planned - done
        pluralStringResource(R.plurals.workout_finish_dialog_incomplete, missing, done, planned, missing)
    } else {
        stringResource(R.string.workout_finish_dialog_logged, uiState.completedSets.count { !it.isWarmup })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_finish_dialog_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.workout_finish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_cancel)) }
        }
    )
}

@Composable
internal fun ExercisePicker(query: String, results: List<Exercise>, onQueryChange: (String) -> Unit, onExercisePick: (Exercise) -> Unit) {
    HdCard {
        Text(
            text = stringResource(R.string.workout_add_exercise),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        HdSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.workout_search_exercises)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            items(items = results, key = { it.id }) { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExercisePick(exercise) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HdExerciseThumbnail(exercise = exercise, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.displayName.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(exercise.muscleGroup.nameResId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** Plates per side for a target load, in the unit of the equipment (kg or lb plates and bars). */
@Composable
internal fun PlateCalculatorDialog(unit: WeightUnit, initialWeight: String, onDismiss: () -> Unit) {
    var targetInput by remember { mutableStateOf(initialWeight) }
    var barWeight by remember { mutableFloatStateOf(unit.barPresets().first()) }

    Dialog(onDismissRequest = onDismiss) {
        HdCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.workout_plate_calculator),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it },
                label = { Text(stringResource(R.string.workout_plate_target, unit.label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.workout_plate_bar),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unit.barPresets().forEach { bar ->
                    HdChip(
                        text = "${bar.formatDecimal()} ${unit.label}",
                        isSelected = barWeight == bar,
                        onClick = { barWeight = bar }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val target = targetInput.replace(',', '.').toFloatOrNull()
            val plates = target?.let { calculatePlatesPerSide(it, barWeight, unit.standardPlates()) }
            Text(
                text = when {
                    target == null -> stringResource(R.string.workout_plate_enter_target)
                    plates.isNullOrEmpty() -> stringResource(R.string.workout_plate_none)
                    else -> stringResource(R.string.workout_plate_per_side, plates.joinToString(" + ") { it.formatDecimal() }, unit.label)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            HdButton(text = stringResource(R.string.workout_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
