package com.example.heavyduty.feature.workout.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The current exercise's sets: logged, being entered and still pending.

private val SetColumnWidth = 44.dp
private val ValueColumnWidth = 76.dp
private val ActionColumnWidth = 48.dp

@Composable
internal fun SetTable(
    uiState: ActiveWorkoutUiState,
    routineExercise: RoutineExercise,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = routineExercise.exercise
    val unit = uiState.currentUnit
    val weightHeader = if (exercise?.isBodyweight == true) "+${unit.label.uppercase()}" else unit.label.uppercase()
    val loggedSets = uiState.setsFor(routineExercise.exerciseId).sortedBy { it.setNumber }
    val currentSetNumber = uiState.currentSetNumber
    // Sets already logged when the exercise is opened just appear; ones logged now animate in.
    val shownSetIds = remember(routineExercise.exerciseId) { loggedSets.mapTo(HashSet()) { it.id } }

    HdCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        TableRow {
            HeaderCell(stringResource(R.string.workout_col_set), Modifier.width(SetColumnWidth))
            HeaderCell(stringResource(R.string.workout_col_previous), Modifier.weight(1f))
            UnitToggleHeader(
                label = weightHeader,
                onToggle = { onEvent(ActiveWorkoutUiEvent.OnToggleUnit) },
                modifier = Modifier.width(ValueColumnWidth)
            )
            HeaderCell(
                stringResource(if (uiState.isTimedInput) R.string.workout_col_time else R.string.workout_col_reps),
                Modifier.width(ValueColumnWidth)
            )
            Spacer(modifier = Modifier.width(ActionColumnWidth))
        }
        Spacer(modifier = Modifier.height(4.dp))

        loggedSets.forEach { set ->
            // Keyed so a row's swipe and entrance state stay with its set when another is removed.
            key(set.id) {
                val isNew = set.id !in shownSetIds
                SideEffect { shownSetIds += set.id }
                LoggedSetRow(
                    set = set,
                    previous = uiState.previousSets[set.setNumber],
                    unit = unit,
                    animateIn = isNew,
                    onDelete = { onEvent(ActiveWorkoutUiEvent.OnDeleteSet(set)) }
                )
            }
        }

        ActiveSetRow(
            uiState = uiState,
            setNumber = currentSetNumber,
            previous = uiState.previousSets[currentSetNumber],
            unit = unit,
            onEvent = onEvent
        )

        // Planned sets still ahead, so the lifter sees what's left of this exercise.
        for (setNumber in (currentSetNumber + 1)..routineExercise.targetSets) {
            PendingSetRow(setNumber = setNumber, previous = uiState.previousSets[setNumber], unit = unit)
        }
    }
}

@Composable
private fun TableRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    FitText(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        ),
        // A floor in dp, not sp: with a large system font the sp floor grew too and a one-word
        // header ("ANTERIOR") couldn't fit nor wrap. The values below keep the user's size.
        minFontSize = with(LocalDensity.current) { MIN_HEADER_FONT.toSp() },
        modifier = modifier
    )
}

@Composable
private fun ValueCell(text: String, modifier: Modifier = Modifier, dim: Boolean = false, bold: Boolean = false, maxLines: Int = 1) {
    FitText(
        text = text,
        style = MaterialTheme.typography.bodyMedium.tabular().copy(
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        maxLines = maxLines,
        modifier = modifier
    )
}

/**
 * Text that shrinks to fit its column instead of cutting itself off. The value columns keep a
 * fixed width (they hold the inputs), so on a narrow phone "Anterior" gets little room: there
 * "8 × 62.5 kg" read as "8 × 62…" and the header as "ANTERIO". Below [MIN_CELL_FONT] it wraps,
 * up to [maxLines], and only then ellipsizes.
 */
@Composable
private fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minFontSize: TextUnit = MIN_CELL_FONT
) {
    if (maxLines == 1) {
        ShrinkingText(text = text, style = style, maxLines = 1, minFontSize = minFontSize, modifier = modifier)
        return
    }
    // A second line only once even the smallest size can't hold it on one: with lines to spare,
    // auto-size would rather wrap at full size than shrink, and every row would grow.
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val fitsOnOneLine = !measurer.measure(
            text = text,
            style = style.copy(fontSize = minFontSize),
            maxLines = 1,
            constraints = Constraints(maxWidth = constraints.maxWidth)
        ).hasVisualOverflow
        ShrinkingText(
            text = text,
            style = style,
            maxLines = if (fitsOnOneLine) 1 else maxLines,
            minFontSize = minFontSize,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ShrinkingText(text: String, style: TextStyle, maxLines: Int, minFontSize: TextUnit, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = style.fontSize, stepSize = 0.5.sp),
        modifier = modifier
    )
}

private val MIN_CELL_FONT = 9.sp
private val MIN_HEADER_FONT = 9.dp

/** "4 × 117.5 kg" — a set from another unit — may need a second line where the column is narrow. */
private const val PREVIOUS_MAX_LINES = 2

@Composable
private fun SetNumberCell(label: String, highlighted: Boolean) {
    Box(modifier = Modifier.width(SetColumnWidth), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * A logged set. Swipe it left to delete (the trash button does the same); a set logged just
 * now drops into place with a light bounce and a confirming vibration.
 */
@Composable
private fun LoggedSetRow(set: WorkoutSet, previous: WorkoutSet?, unit: WeightUnit, animateIn: Boolean, onDelete: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var deleteRequested by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(positionalThreshold = { distance -> distance * 0.4f })
    // On success the set leaves the list and this row with it. If it's still here a moment
    // later the delete failed (a message says so), so slide the row back.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            delay(DELETE_ROLLBACK_MS)
            deleteRequested = false
            dismissState.reset()
        }
    }

    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !deleteRequested) {
                deleteRequested = true
                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                onDelete()
            }
        },
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        // Drawing-phase only (no relayout): fades and drops the row in from slightly above.
        modifier = Modifier.graphicsLayer {
            val progress = appear.value
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * -10.dp.toPx()
            scaleX = 0.96f + 0.04f * progress
            scaleY = scaleX
        }
    ) {
        LoggedSetRowContent(set = set, previous = previous, unit = unit, onDelete = onDelete)
    }
}

@Composable
private fun LoggedSetRowContent(set: WorkoutSet, previous: WorkoutSet?, unit: WeightUnit, onDelete: () -> Unit) {
    TableRow(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            // Opaque, so the delete background doesn't tint the row while it's swiped.
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    .compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
            )
    ) {
        SetNumberCell(
            label = if (set.isWarmup) stringResource(R.string.workout_warmup_short) else set.setNumber.toString(),
            highlighted = false
        )
        ValueCell(previous.describe(unit), Modifier.weight(1f), dim = true, maxLines = PREVIOUS_MAX_LINES)
        // Each set shows in the unit it was logged in — a lb machine set stays "50 lb".
        val setUnit = set.weightUnit ?: unit
        ValueCell(
            text =
                set.weight?.kgToRounded(setUnit)?.let { "${it.formatDecimal()}${if (setUnit != unit) " ${setUnit.label}" else ""}" } ?: "—",
            modifier = Modifier.width(ValueColumnWidth),
            bold = true
        )
        val effortSuffix = when (val effort = set.effort) {
            is Effort.Rpe -> " @${effort.value.formatDecimal()}"
            is Effort.Rir -> " (${effort.value})"
            null -> ""
        }
        ValueCell(
            text = (set.durationSecs?.let { "$it s" } ?: set.reps?.toString() ?: "—") + effortSuffix,
            modifier = Modifier.width(ValueColumnWidth),
            bold = true
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(ActionColumnWidth)) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = stringResource(R.string.workout_delete_set),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActiveSetRow(
    uiState: ActiveWorkoutUiState,
    setNumber: Int,
    previous: WorkoutSet?,
    unit: WeightUnit,
    onEvent: (ActiveWorkoutUiEvent) -> Unit
) {
    val hint = uiState.currentSetHint
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(vertical = 4.dp)
    ) {
        TableRow {
            SetNumberCell(
                label = if (uiState.isWarmupInput) stringResource(R.string.workout_warmup_short) else setNumber.toString(),
                highlighted = true
            )
            ValueCell(previous.describe(unit), Modifier.weight(1f), dim = true, maxLines = PREVIOUS_MAX_LINES)
            CompactNumberInput(
                value = uiState.weightInput,
                onValueChange = { onEvent(ActiveWorkoutUiEvent.OnWeightChange(it)) },
                placeholder = hint.weightKg?.kgToRounded(unit)?.formatDecimal() ?: "0",
                keyboardType = KeyboardType.Decimal,
                width = ValueColumnWidth
            )
            if (uiState.isTimedInput) {
                HoldTimerCell(
                    elapsedSeconds = uiState.workTimerElapsedSecs,
                    placeholderSeconds = hint.durationSecs,
                    isRunning = uiState.isWorkTimerRunning,
                    onToggle = { onEvent(ActiveWorkoutUiEvent.OnToggleWorkTimer) }
                )
            } else {
                CompactNumberInput(
                    value = uiState.repsInput,
                    onValueChange = { onEvent(ActiveWorkoutUiEvent.OnRepsChange(it)) },
                    placeholder = hint.reps?.toString() ?: "0",
                    keyboardType = KeyboardType.Number,
                    width = ValueColumnWidth
                )
            }
            LogSetButton(enabled = !uiState.isLoggingSet, onLog = { onEvent(ActiveWorkoutUiEvent.OnLogSet) })
        }
        if (uiState.effortTrackingEnabled) {
            EffortInputRow(
                scale = uiState.preferredEffortScale,
                value = uiState.effortInput,
                onValueChange = { onEvent(ActiveWorkoutUiEvent.OnEffortInputChange(it)) }
            )
        }
    }
}

/** Squeezes and springs back on tap; the new row's arrival confirms the set. */
@Composable
private fun LogSetButton(enabled: Boolean, onLog: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(1f) }
    FilledIconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            scope.launch {
                press.snapTo(0.8f)
                press.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            onLog()
        },
        enabled = enabled,
        modifier = modifier
            .size(ActionColumnWidth)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
    ) {
        Icon(imageVector = Icons.Default.Check, contentDescription = stringResource(R.string.workout_log_set))
    }
}

/** The RIR / RPE field under the set, when effort tracking is on. */
@Composable
private fun EffortInputRow(scale: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SetColumnWidth + 6.dp, top = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = scale, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        CompactNumberInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = "—",
            keyboardType = KeyboardType.Decimal,
            width = 64.dp
        )
    }
}

@Composable
private fun PendingSetRow(setNumber: Int, previous: WorkoutSet?, unit: WeightUnit) {
    TableRow {
        SetNumberCell(label = setNumber.toString(), highlighted = false)
        ValueCell(previous.describe(unit), Modifier.weight(1f), dim = true, maxLines = PREVIOUS_MAX_LINES)
        ValueCell("—", Modifier.width(ValueColumnWidth), dim = true)
        ValueCell("—", Modifier.width(ValueColumnWidth), dim = true)
        Spacer(modifier = Modifier.width(ActionColumnWidth))
    }
}

@Composable
private fun CompactNumberInput(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType, width: Dp) {
    val textStyle = MaterialTheme.typography.bodyLarge.tabular().copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    BasicTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')) },
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.width(width),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun HoldTimerCell(elapsedSeconds: Int, placeholderSeconds: Int?, isRunning: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .width(ValueColumnWidth)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = stringResource(if (isRunning) R.string.workout_hold_stop else R.string.workout_hold_start),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        val showPlaceholder = elapsedSeconds == 0 && !isRunning && placeholderSeconds != null
        Text(
            text = if (showPlaceholder) "${placeholderSeconds}s" else "${elapsedSeconds}s",
            style = MaterialTheme.typography.bodyLarge.tabular(),
            fontWeight = FontWeight.SemiBold,
            color = if (showPlaceholder) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
internal fun SetOptions(
    uiState: ActiveWorkoutUiState,
    routineExercise: RoutineExercise,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPlateCalculator by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HdChip(
            text = stringResource(R.string.workout_warmup),
            isSelected = uiState.isWarmupInput,
            onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleWarmupInput) }
        )
        HdChip(
            text = stringResource(R.string.workout_timed),
            isSelected = uiState.isTimedInput,
            onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleTimedInput) }
        )
        if (routineExercise.exercise?.isBodyweight != true) {
            HdChip(
                text = stringResource(R.string.workout_plates),
                isSelected = false,
                onClick = { showPlateCalculator = true }
            )
        }
    }
    if (showPlateCalculator) {
        val unit = uiState.currentUnit
        val initial = uiState.weightInput.toFloatOrNull() ?: uiState.currentSetHint.weightKg?.kgToRounded(unit)
        PlateCalculatorDialog(
            unit = unit,
            initialWeight = initial?.formatDecimal().orEmpty(),
            onDismiss = { showPlateCalculator = false }
        )
    }
}

private const val DELETE_ROLLBACK_MS = 1_500L

/** Header of the weight column; tapping it switches the exercise between kg and lb. */
@Composable
private fun UnitToggleHeader(label: String, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = stringResource(R.string.workout_toggle_unit),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(14.dp)
        )
    }
}

/** How a previous set reads in the "Anterior" column: "8 × 60", "45 s" or "—". */
private fun WorkoutSet?.describe(unit: WeightUnit): String {
    if (this == null) return "—"
    val setUnit = weightUnit ?: unit
    val weightPart = weight?.takeIf { it > 0f }?.kgToRounded(setUnit)?.formatDecimal()
        ?.let { if (setUnit != unit) "$it ${setUnit.label}" else it }
    return when {
        durationSecs != null -> "$durationSecs s" + (weightPart?.let { " × $it" } ?: "")
        reps != null && weightPart != null -> "$reps × $weightPart"
        reps != null -> "$reps"
        else -> "—"
    }
}
