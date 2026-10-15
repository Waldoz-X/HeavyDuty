package com.example.heavyduty.feature.workout.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.Prescription
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.isPerSide
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdRollingText
import com.example.heavyduty.core.ui.nameResId

// Everything above the set table: workout header, rest timer, exercise strip and suggestion.

@Composable
internal fun WorkoutHeader(uiState: ActiveWorkoutUiState, onFinish: () -> Unit, onDiscard: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SessionTitleAndClock(name = uiState.sessionName, elapsedSeconds = uiState.elapsedSeconds, modifier = Modifier.weight(1f))
                HdButton(text = stringResource(R.string.workout_finish), onClick = onFinish, icon = Icons.Default.Check)
                WorkoutOptionsMenu(onDiscard = onDiscard)
            }
            Spacer(modifier = Modifier.height(8.dp))
            WorkoutProgress(uiState = uiState)
        }
    }
}

@Composable
private fun SessionTitleAndClock(name: String, elapsedSeconds: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = name.ifBlank { stringResource(R.string.workout_default_name) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatDuration(elapsedSeconds),
            style = MaterialTheme.typography.headlineSmall.tabular(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Planned sets done, as a bar and "3/6 series" — or, in a free workout, just how many were logged. */
@Composable
private fun WorkoutProgress(uiState: ActiveWorkoutUiState, modifier: Modifier = Modifier) {
    val planned = uiState.plannedSetCount
    Column(modifier = modifier) {
        if (planned > 0) {
            val done = uiState.completedPlannedSetCount
            val progress by animateFloatAsState(
                targetValue = done.toFloat() / planned,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "workoutProgress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                // A muted track, so 0/6 reads as empty rather than as a full bar.
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Spacer(modifier = Modifier.height(4.dp))
            HdRollingText(
                text = stringResource(R.string.workout_sets_progress, done, planned),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val logged = uiState.completedSets.count { !it.isWarmup }
            Text(
                text = pluralStringResource(R.plurals.workout_sets_logged, logged, logged),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The menu beside "Finalizar": the rarer, destructive action sits one tap further away than finishing. */
@Composable
private fun WorkoutOptionsMenu(onDiscard: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.workout_more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_discard), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDiscard()
                }
            )
        }
    }
}

/**
 * The rest countdown. The bar glides continuously instead of jumping once a second, the
 * digits roll, and the last [REST_WARNING_SECONDS] seconds turn it to the accent color with a
 * tick each second so the lifter can get set without watching the screen.
 */
@Composable
internal fun RestTimerBanner(rest: RestTimerState, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val ending = rest.remainingSeconds in 1..REST_WARNING_SECONDS
    LaunchedEffect(rest.remainingSeconds) {
        if (ending) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
    val containerColor by animateColorAsState(
        if (ending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        label = "restContainer"
    )
    val contentColor by animateColorAsState(
        if (ending) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "restContent"
    )
    val barColor by animateColorAsState(
        if (ending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        label = "restBar"
    )
    // Each one-second step glides over a short moment and then holds: spread over the whole
    // second instead, the screen redrew every frame for the entire rest.
    val progress by animateFloatAsState(
        targetValue = rest.remainingSeconds.toFloat() / rest.totalSeconds.coerceAtLeast(1),
        animationSpec = tween(durationMillis = REST_BAR_GLIDE_MILLIS, easing = FastOutSlowInEasing),
        label = "restProgress"
    )

    Surface(color = containerColor, contentColor = contentColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            RestCountdown(remainingSeconds = rest.remainingSeconds, contentColor = contentColor, onAdjust = onAdjust, onSkip = onSkip)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = barColor,
                trackColor = contentColor.copy(alpha = 0.15f),
                drawStopIndicator = {}
            )
        }
    }
}

private const val REST_WARNING_SECONDS = 3
private const val REST_BAR_GLIDE_MILLIS = 300

/** "Descanso 1:15", then −15 s / +15 s and skip. */
@Composable
private fun RestCountdown(
    remainingSeconds: Int,
    contentColor: Color,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.workout_rest), style = MaterialTheme.typography.labelMedium)
            HdRollingText(
                text = formatDuration(remainingSeconds),
                style = MaterialTheme.typography.titleLarge.tabular(),
                fontWeight = FontWeight.Bold,
                countingDown = true
            )
        }
        SmallTonalButton(text = stringResource(R.string.workout_rest_minus), onClick = { onAdjust(-REST_STEP_SECONDS) })
        Spacer(modifier = Modifier.width(6.dp))
        SmallTonalButton(text = stringResource(R.string.workout_rest_plus), onClick = { onAdjust(REST_STEP_SECONDS) })
        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.workout_rest_skip), color = contentColor)
        }
    }
}

/** A compact pill tinted from the surrounding content color, so it follows the banner's color. */
@Composable
private fun SmallTonalButton(text: String, onClick: () -> Unit) {
    val contentColor = LocalContentColor.current
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = contentColor.copy(alpha = 0.12f),
            contentColor = contentColor
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun ExerciseStrip(uiState: ActiveWorkoutUiState, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.currentExerciseIndex) {
        listState.animateScrollToItem(uiState.currentExerciseIndex)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(uiState.routineExercises) { index, exercise ->
            val done = uiState.setsFor(exercise.exerciseId).count { !it.isWarmup }
            ExerciseStripChip(
                name = exercise.exercise?.displayName.orEmpty(),
                progress = if (exercise.targetSets > 0) "$done/${exercise.targetSets}" else "$done",
                isSelected = index == uiState.currentExerciseIndex,
                isDone = uiState.isDone(exercise),
                onClick = { onSelect(index) }
            )
        }
    }
}

/** One exercise in the strip: its name and sets done, highlighted when it's the current one. */
@Composable
private fun ExerciseStripChip(
    name: String,
    progress: String,
    isSelected: Boolean,
    isDone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "stripContainer"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "stripContent"
    )
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                // Cut in the middle: names of one family differ at the end ("Press de banca con
                // barra" / "… con mancuernas"), which an end ellipsis hid on narrow phones.
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The check pops in when the last planned set is logged.
                AnimatedVisibility(
                    visible = isDone,
                    enter = expandHorizontally() +
                        scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) +
                        fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.workout_done),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                HdRollingText(text = progress, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun CurrentExerciseHeader(
    routineExercise: RoutineExercise,
    index: Int,
    count: Int,
    isFreeWorkout: Boolean,
    onSelect: (Int) -> Unit
) {
    val exercise = routineExercise.exercise
    val unit = routineExercise.displayUnit
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(index - 1) }, enabled = index > 0) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.workout_previous_exercise_cd)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = exercise?.displayName.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val details = buildList {
                exercise?.muscleGroup?.let { add(stringResource(it.nameResId)) }
                if (!isFreeWorkout && routineExercise.targetSets > 0) {
                    val weight = routineExercise.targetWeight.takeIf { it > 0f }
                        ?.let { " · ${it.kgToRounded(unit).formatDecimal()} ${unit.label}" }
                        .orEmpty()
                    add("${routineExercise.targetSets} × ${routineExercise.targetReps}$weight")
                    if (exercise?.isPerSide == true) {
                        add(stringResource(R.string.workout_per_side, routineExercise.targetReps / 2))
                    }
                    add(stringResource(R.string.workout_rest_between, routineExercise.restBetweenSetsSecs))
                }
            }
            Text(
                text = details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = { onSelect(index + 1) }, enabled = index < count - 1) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.workout_next_exercise_cd)
            )
        }
    }
}

@Composable
internal fun SuggestionRow(suggestion: Prescription, weightUnit: WeightUnit, onUse: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (suggestion.isDeload) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (suggestion.isDeload) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val target = buildString {
        append("${suggestion.sets} × ")
        val durationSecs = suggestion.durationSecs
        val weightKg = suggestion.weightKg?.takeIf { it > 0f }
        when {
            durationSecs != null -> append("$durationSecs s")
            weightKg != null -> append("${suggestion.reps} · ${weightKg.kgToRounded(weightUnit).formatDecimal()} ${weightUnit.label}")
            else -> append("${suggestion.reps}")
        }
    }
    Surface(color = containerColor, contentColor = contentColor, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.workout_suggestion, target) +
                        if (suggestion.isDeload) " · ${stringResource(R.string.workout_deload)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                suggestion.reasonNote?.let { note ->
                    Text(text = note, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onUse) {
                Text(stringResource(R.string.workout_suggestion_use), color = contentColor)
            }
        }
    }
}

private const val REST_STEP_SECONDS = 15

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
