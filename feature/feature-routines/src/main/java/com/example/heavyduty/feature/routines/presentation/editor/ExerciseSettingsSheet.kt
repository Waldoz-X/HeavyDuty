package com.example.heavyduty.feature.routines.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.model.loadStep
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdExerciseThumbnail
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.nameResId
import kotlin.math.roundToInt

// The bottom sheet with one routine exercise's targets: sets, reps, weight, rest and progression.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseSettingsSheet(routineExercise: RoutineExercise, onUpdate: (RoutineExercise) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exercise = routineExercise.exercise
    // kg/lb of the machine or plates used for this exercise in this routine.
    val unit = routineExercise.displayUnit
    val isTimeBased = routineExercise.progressionPolicy == ProgressionPolicy.TIME

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SheetHeader(exercise = exercise)

            TargetsRow(routineExercise = routineExercise, isTimeBased = isTimeBased, onUpdate = onUpdate)

            WeightSection(routineExercise = routineExercise, unit = unit, onUpdate = onUpdate)

            RestSection(routineExercise = routineExercise, onUpdate = onUpdate)

            AdvancedSection(routineExercise = routineExercise, isTimeBased = isTimeBased, onUpdate = onUpdate)

            HdTonalButton(
                text = stringResource(R.string.routine_done),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SheetHeader(exercise: Exercise?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HdExerciseThumbnail(exercise = exercise, size = 56.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise?.displayName.orEmpty().capitalized(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            exercise?.muscleGroup?.let {
                Text(
                    text = stringResource(it.nameResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Sets, and reps — or the hold time for a timed exercise. */
@Composable
private fun TargetsRow(routineExercise: RoutineExercise, isTimeBased: Boolean, onUpdate: (RoutineExercise) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberStepper(
            label = stringResource(R.string.routine_sets),
            value = routineExercise.targetSets.toFloat(),
            step = 1f,
            range = 1f..20f,
            onValueChange = { onUpdate(routineExercise.copy(targetSets = it.roundToInt())) },
            modifier = Modifier.weight(1f)
        )
        if (isTimeBased) {
            NumberStepper(
                label = stringResource(R.string.routine_duration),
                value = routineExercise.targetDurationSecs.toFloat(),
                step = 5f,
                range = 5f..600f,
                onValueChange = { onUpdate(routineExercise.copy(targetDurationSecs = it.roundToInt())) },
                modifier = Modifier.weight(1f)
            )
        } else {
            NumberStepper(
                label = stringResource(R.string.routine_reps),
                value = routineExercise.targetReps.toFloat(),
                step = 1f,
                range = 1f..100f,
                onValueChange = { onUpdate(routineExercise.withTargetReps(it.roundToInt())) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Target weight, entered in the unit chosen for this exercise in this routine. */
@Composable
private fun WeightSection(routineExercise: RoutineExercise, unit: WeightUnit, onUpdate: (RoutineExercise) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.routine_weight), Modifier.weight(1f))
            UnitToggle(
                unit = unit,
                onUnitChange = { newUnit -> onUpdate(routineExercise.copy(weightUnit = newUnit)) }
            )
        }
        NumberStepper(
            label = null,
            value = routineExercise.targetWeight.kgToRounded(unit),
            step = unit.loadStep,
            range = 0f..1000f,
            decimals = true,
            suffix = unit.label,
            onValueChange = { onUpdate(routineExercise.copy(targetWeight = it.toKgFrom(unit))) },
            modifier = Modifier.fillMaxWidth()
        )
        if (routineExercise.exercise?.isBodyweight == true) {
            Text(
                text = stringResource(R.string.routine_bodyweight_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RestSection(routineExercise: RoutineExercise, onUpdate: (RoutineExercise) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.routine_rest))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            REST_PRESETS_SECONDS.forEach { seconds ->
                HdChip(
                    text = formatRest(seconds),
                    isSelected = routineExercise.restBetweenSetsSecs == seconds,
                    onClick = { onUpdate(routineExercise.copy(restBetweenSetsSecs = seconds)) }
                )
            }
        }
        NumberStepper(
            label = null,
            value = routineExercise.restBetweenSetsSecs.toFloat(),
            step = 15f,
            range = 0f..600f,
            format = { formatRest(it.roundToInt()) },
            onValueChange = { onUpdate(routineExercise.copy(restBetweenSetsSecs = it.roundToInt())) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Collapsed by default: the progression policy and its rep range and stall threshold. */
@Composable
private fun AdvancedSection(routineExercise: RoutineExercise, isTimeBased: Boolean, onUpdate: (RoutineExercise) -> Unit) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { showAdvanced = !showAdvanced },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(stringResource(R.string.routine_advanced))
                    Text(
                        text = stringResource(routineExercise.progressionPolicy.nameResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (showAdvanced) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ProgressionPolicy.entries.toList()) { policy ->
                        HdChip(
                            text = stringResource(policy.nameResId),
                            isSelected = routineExercise.progressionPolicy == policy,
                            onClick = { onUpdate(routineExercise.copy(progressionPolicy = policy)) }
                        )
                    }
                }
                if (routineExercise.progressionPolicy != ProgressionPolicy.OFF) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberStepper(
                            label = stringResource(
                                if (isTimeBased) R.string.routine_time_range_min else R.string.routine_rep_range_min
                            ),
                            value = routineExercise.repRangeLow.toFloat(),
                            step = if (isTimeBased) 5f else 1f,
                            range = 1f..routineExercise.repRangeHigh.toFloat(),
                            onValueChange = { onUpdate(routineExercise.copy(repRangeLow = it.roundToInt())) },
                            modifier = Modifier.weight(1f)
                        )
                        NumberStepper(
                            label = stringResource(
                                if (isTimeBased) R.string.routine_time_range_max else R.string.routine_rep_range_max
                            ),
                            value = routineExercise.repRangeHigh.toFloat(),
                            step = if (isTimeBased) 5f else 1f,
                            range = routineExercise.repRangeLow.toFloat()..600f,
                            onValueChange = { onUpdate(routineExercise.copy(repRangeHigh = it.roundToInt())) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val showStall = routineExercise.progressionPolicy == ProgressionPolicy.LINEAR ||
                        routineExercise.progressionPolicy == ProgressionPolicy.GREYSKULL_LP
                    if (showStall) {
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberStepper(
                            label = stringResource(R.string.routine_stall_threshold),
                            value = routineExercise.stallThreshold.toFloat(),
                            step = 1f,
                            range = 1f..10f,
                            onValueChange = { onUpdate(routineExercise.copy(stallThreshold = it.roundToInt())) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitToggle(unit: WeightUnit, onUnitChange: (WeightUnit) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        listOf(WeightUnit.KG, WeightUnit.LB).forEachIndexed { index, option ->
            SegmentedButton(
                selected = unit == option,
                onClick = { onUnitChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                label = { Text(option.label) }
            )
        }
    }
}

/**
 * − value + control: taps for quick changes, and the value itself is editable for typing an
 * exact number (e.g. 62.5).
 */
@Composable
private fun NumberStepper(
    label: String?,
    value: Float,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Boolean = false,
    suffix: String? = null,
    format: ((Float) -> String)? = null
) {
    val display: (Float) -> String = format ?: { if (decimals) it.formatDecimal() else it.roundToInt().toString() }
    fun textOf(v: Float) = display(v).let { TextFieldValue(it, TextRange(it.length)) }
    var field by remember { mutableStateOf(textOf(value)) }
    var isFocused by remember { mutableStateOf(false) }
    // The tap that focuses the field also places the cursor, after onFocusChanged runs — so the
    // "select everything" is applied to that first cursor placement instead.
    var selectAllOnTap by remember { mutableStateOf(false) }
    // Follow outside changes (a rest preset, a unit switch) — but never while the user is typing,
    // or the half-typed text would be replaced mid-keystroke.
    LaunchedEffect(value, isFocused) {
        if (!isFocused && field.text.replace(',', '.').toFloatOrNull() != value) field = textOf(value)
    }
    // − / + set the number and the text together, focused or not.
    val stepBy: (Float) -> Unit = { delta ->
        val next = snap(value + delta, step).coerceIn(range)
        field = textOf(next)
        onValueChange(next)
    }
    val editable = format == null

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { stepBy(-step) },
                enabled = value > range.start
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.routine_decrease))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFeatureSettings = "tnum"
                )
                if (editable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = field,
                            onValueChange = { input ->
                                if (selectAllOnTap && input.text == field.text) {
                                    selectAllOnTap = false
                                    field = field.copy(selection = TextRange(0, field.text.length))
                                    return@BasicTextField
                                }
                                selectAllOnTap = false
                                val cleaned = input.text.filter { it.isDigit() || (decimals && (it == '.' || it == ',')) }.replace(',', '.')
                                field = if (cleaned == input.text) input else TextFieldValue(cleaned, TextRange(cleaned.length))
                                cleaned.toFloatOrNull()?.let { onValueChange(it.coerceIn(range)) }
                            },
                            singleLine = true,
                            textStyle = style,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            // Tapping selects the whole number, so typing replaces it instead of
                            // landing mid-number ("0" + "180" used to become "1800").
                            modifier = Modifier
                                .width(IntrinsicTextWidth)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused && !isFocused) {
                                        field = field.copy(selection = TextRange(0, field.text.length))
                                        selectAllOnTap = true
                                    }
                                    isFocused = focus.isFocused
                                }
                        )
                        if (suffix != null) {
                            Text(
                                text = suffix,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(text = display(value), style = style)
                }
            }
            FilledTonalIconButton(
                onClick = { stepBy(step) },
                enabled = value < range.endInclusive
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.routine_increase))
            }
        }
    }
}

private val IntrinsicTextWidth = 88.dp

/** Rounds to the step grid, so + and − land on round numbers (62.5 → 65, not 64.9). */
private fun snap(value: Float, step: Float): Float = (value / step).roundToInt() * step

/** Keeps a range the progression uses in step with a changed rep target it was derived from. */
private fun RoutineExercise.withTargetReps(reps: Int): RoutineExercise {
    val linked = repRangeHigh == targetReps
    return copy(
        targetReps = reps,
        repRangeHigh = if (linked) reps else repRangeHigh,
        repRangeLow = if (linked) (reps - 2).coerceAtLeast(1) else repRangeLow
    )
}

private val REST_PRESETS_SECONDS = listOf(60, 90, 120, 180)
