package com.example.heavyduty.feature.workout.presentation

import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalResources
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import com.example.heavyduty.core.ui.components.HdRollingText
import kotlinx.coroutines.delay
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.Effort
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.Prescription
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.calculatePlatesPerSide
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.domain.model.isPerSide
import com.example.heavyduty.core.domain.model.barPresets
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.model.standardPlates
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdExerciseThumbnail
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdSearchBar
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.nameResId
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    onNavigateToRoutines: () -> Unit,
    onWorkoutCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var personalRecord by remember { mutableStateOf<ActiveWorkoutUiEffect.PersonalRecord?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ActiveWorkoutUiEffect.NavigateToRoutines -> onNavigateToRoutines()
                ActiveWorkoutUiEffect.WorkoutCompleted -> onWorkoutCompleted()
                // Shown in its own coroutine so a visible snackbar never holds up navigation.
                is ActiveWorkoutUiEffect.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageResId))
                }
                is ActiveWorkoutUiEffect.SetDeleted -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.workout_set_deleted),
                        actionLabel = resources.getString(R.string.workout_undo),
                        // Long enough to notice a mistaken swipe mid-workout and reach the button.
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(ActiveWorkoutUiEvent.OnUndoDeleteSet(effect.set))
                    }
                }
                is ActiveWorkoutUiEffect.PersonalRecord -> {
                    personalRecord = effect
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    // The rest countdown lives in a notification while the phone is pocketed or in another
    // app; on Android 13+ that needs the permission, so ask once when a workout starts.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.isActive) {
        if (uiState.isActive && !askedForNotifications &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Keep the screen on while a session is active, so a rest period between sets doesn't
    // lock the phone and lose the lifter's place.
    val view = LocalView.current
    DisposableEffect(uiState.isActive) {
        view.keepScreenOn = uiState.isActive
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> HdLoadingIndicator(message = stringResource(R.string.workout_loading))
            uiState.isActive -> ActiveWorkoutContent(uiState = uiState, onEvent = viewModel::onEvent)
            else -> WorkoutStartScreen(routines = uiState.routines, onEvent = viewModel::onEvent)
        }
        PersonalRecordBanner(
            record = personalRecord,
            onDismiss = { personalRecord = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

/**
 * Drops in from the top when a set beats the exercise's best, then leaves on its own (or on
 * tap). It overlays the screen instead of pushing it down, so the set table doesn't jump.
 */
@Composable
private fun PersonalRecordBanner(
    record: ActiveWorkoutUiEffect.PersonalRecord?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keeps drawing the last record while the banner animates out.
    var shown by remember { mutableStateOf(record) }
    if (record != null) shown = record
    LaunchedEffect(record) {
        if (record != null) {
            delay(PR_BANNER_MS)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = record != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { -it } +
            fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val pr = shown ?: return@AnimatedVisibility
        Surface(
            onClick = onDismiss,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The trophy pops once the banner has landed.
                val trophyScale = remember(pr) { Animatable(0.3f) }
                LaunchedEffect(pr) {
                    trophyScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
                }
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = trophyScale.value
                            scaleY = trophyScale.value
                        },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.workout_pr_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.workout_pr_detail,
                            pr.exerciseName,
                            pr.estimatedOneRepMaxKg.kgToRounded(pr.unit).formatDecimal(),
                            pr.unit.label,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private const val PR_BANNER_MS = 3_500L

// ============================================================== start screen

@Composable
private fun WorkoutStartScreen(
    routines: List<Routine>,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.workout_start_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.workout_start_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            HdCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onEvent(ActiveWorkoutUiEvent.OnStartFreeWorkout) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Default.FitnessCenter)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.workout_free_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.workout_free_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.workout_your_routines),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (routines.isNotEmpty()) {
                    HdTextButton(
                        text = stringResource(R.string.workout_manage_routines),
                        onClick = { onEvent(ActiveWorkoutUiEvent.OnNavigateToRoutines) },
                    )
                }
            }
        }

        if (routines.isEmpty()) {
            item {
                HdEmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = stringResource(R.string.workout_no_routines_title),
                    description = stringResource(R.string.workout_no_routines_description),
                )
                Spacer(modifier = Modifier.height(12.dp))
                HdTonalButton(
                    text = stringResource(R.string.workout_create_routine),
                    onClick = { onEvent(ActiveWorkoutUiEvent.OnNavigateToRoutines) },
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(items = routines, key = { it.id }) { routine ->
                RoutineStartCard(
                    routine = routine,
                    onStart = { onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id)) },
                )
            }
        }
    }
}

@Composable
private fun RoutineStartCard(routine: Routine, onStart: () -> Unit) {
    HdCard(onClick = onStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when {
                    routine.exercises.isNotEmpty() -> pluralStringResource(
                        R.plurals.workout_exercise_count,
                        routine.exercises.size,
                        routine.exercises.size,
                    )
                    else -> routine.description
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            HdButton(
                text = stringResource(R.string.workout_start_action),
                onClick = onStart,
                icon = Icons.Default.PlayArrow,
            )
        }
    }
}

// ============================================================== active workout

/** Internal (not private) so UI tests can drive it with a plain state, no ViewModel or Hilt. */
@Composable
internal fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
) {
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    val currentExercise = uiState.currentExercise

    // A phone on its side is too short to pin the header and the exercise strip above the
    // list (only a sliver of the set table was left), so there they scroll with it. The rest
    // banner stays pinned either way: it's brief and the one thing worth seeing at a glance.
    val compactHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } < COMPACT_HEIGHT
    val showExerciseStrip = uiState.routineExercises.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        if (!compactHeight) {
            WorkoutHeader(uiState = uiState, onFinish = { showFinishDialog = true })
        }

        // Keeps drawing the last rest state while the banner animates out.
        var lastRest by remember { mutableStateOf(uiState.restTimer) }
        if (uiState.restTimer != null) lastRest = uiState.restTimer
        AnimatedVisibility(
            visible = uiState.restTimer != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            lastRest?.let { rest ->
                RestTimerBanner(
                    rest = rest,
                    onAdjust = { onEvent(ActiveWorkoutUiEvent.OnAdjustRest(it)) },
                    onSkip = { onEvent(ActiveWorkoutUiEvent.OnSkipRest) },
                )
            }
        }

        if (!compactHeight && showExerciseStrip) {
            ExerciseStrip(uiState = uiState, onSelect = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(it)) })
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (compactHeight) {
                item(key = "workoutHeader") {
                    Box(modifier = Modifier.bleedHorizontally(16.dp)) {
                        WorkoutHeader(uiState = uiState, onFinish = { showFinishDialog = true })
                    }
                }
                if (showExerciseStrip) {
                    item(key = "exerciseStrip") {
                        Box(modifier = Modifier.bleedHorizontally(16.dp)) {
                            ExerciseStrip(uiState = uiState, onSelect = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(it)) })
                        }
                    }
                }
            }
            if (currentExercise != null) {
                // The exercise's own items are keyed by it, so switching exercises fades the old
                // ones out and the new ones in (animateItem) instead of swapping them abruptly.
                val exerciseKey = currentExercise.exerciseId
                item(key = "header") {
                    AnimatedContent(
                        targetState = uiState.currentExerciseIndex,
                        transitionSpec = {
                            // Slides the way the lifter moved through the routine.
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(tween(EXERCISE_SWITCH_MS)) { direction * it / 4 } + fadeIn(tween(EXERCISE_SWITCH_MS)))
                                .togetherWith(slideOutHorizontally(tween(EXERCISE_SWITCH_MS)) { -direction * it / 4 } + fadeOut(tween(EXERCISE_SWITCH_MS / 2)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "exerciseHeader",
                    ) { index ->
                        val shownExercise = uiState.routineExercises.getOrNull(index) ?: return@AnimatedContent
                        CurrentExerciseHeader(
                            routineExercise = shownExercise,
                            index = index,
                            count = uiState.routineExercises.size,
                            isFreeWorkout = uiState.allowAddingExercises,
                            onSelect = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(it)) },
                        )
                    }
                }
                uiState.suggestedPrescription?.let { suggestion ->
                    item(key = "suggestion-$exerciseKey") {
                        SuggestionRow(
                            suggestion = suggestion,
                            weightUnit = uiState.currentUnit,
                            onUse = { onEvent(ActiveWorkoutUiEvent.OnUseSuggestion) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                item(key = "sets-$exerciseKey") {
                    SetTable(
                        uiState = uiState,
                        routineExercise = currentExercise,
                        onEvent = onEvent,
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "options-$exerciseKey") {
                    SetOptions(
                        uiState = uiState,
                        routineExercise = currentExercise,
                        onEvent = onEvent,
                        modifier = Modifier.animateItem(),
                    )
                }
                nextExerciseIndex(uiState)?.let { nextIndex ->
                    item(key = "next") {
                        val nextName = uiState.routineExercises[nextIndex].exercise?.name.orEmpty()
                        HdTonalButton(
                            text = stringResource(R.string.workout_next_exercise, nextName),
                            onClick = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(nextIndex)) },
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                        )
                    }
                }
            } else if (uiState.allowAddingExercises && !uiState.isPickingExercise) {
                item(key = "empty") {
                    HdEmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = stringResource(R.string.workout_no_exercises_title),
                        description = stringResource(R.string.workout_no_exercises_description),
                    )
                }
            }

            if (uiState.allowAddingExercises) {
                item(key = "picker") {
                    if (uiState.isPickingExercise) {
                        ExercisePicker(
                            query = uiState.exercisePickerQuery,
                            results = uiState.exercisePickerResults,
                            onQueryChange = { onEvent(ActiveWorkoutUiEvent.OnExercisePickerQueryChange(it)) },
                            onExercisePick = { onEvent(ActiveWorkoutUiEvent.OnAddExercise(it)) },
                        )
                    } else {
                        HdTextButton(
                            text = stringResource(R.string.workout_add_exercise),
                            onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleExercisePicker) },
                            icon = Icons.Default.Add,
                        )
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        val haptic = LocalHapticFeedback.current
        FinishWorkoutDialog(
            uiState = uiState,
            onConfirm = {
                showFinishDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(ActiveWorkoutUiEvent.OnCompleteWorkout)
            },
            onDismiss = { showFinishDialog = false },
        )
    }
}

/** The next exercise still short of its planned sets, once the current one has all of them. */
private const val EXERCISE_SWITCH_MS = 260

/** Below this window height (a phone in landscape) the header scrolls with the list. */
private val COMPACT_HEIGHT = 480.dp

/** Lets a list item span the list's side padding, edge to edge like the pinned version. */
private fun Modifier.bleedHorizontally(padding: Dp): Modifier = layout { measurable, constraints ->
    val extra = padding.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
private const val DELETE_ROLLBACK_MS = 1_500L

private fun nextExerciseIndex(uiState: ActiveWorkoutUiState): Int? {
    val current = uiState.currentExercise ?: return null
    if (current.targetSets <= 0 || !uiState.isDone(current)) return null
    val exercises = uiState.routineExercises
    return (1 until exercises.size)
        .map { (uiState.currentExerciseIndex + it) % exercises.size }
        .firstOrNull { !uiState.isDone(exercises[it]) }
}

private fun ActiveWorkoutUiState.isDone(exercise: RoutineExercise): Boolean =
    exercise.targetSets > 0 && setsFor(exercise.exerciseId).count { !it.isWarmup } >= exercise.targetSets

@Composable
private fun WorkoutHeader(uiState: ActiveWorkoutUiState, onFinish: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.sessionName.ifBlank { stringResource(R.string.workout_default_name) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatDuration(uiState.elapsedSeconds),
                        style = MaterialTheme.typography.headlineSmall.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HdButton(
                    text = stringResource(R.string.workout_finish),
                    onClick = onFinish,
                    icon = Icons.Default.Check,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val planned = uiState.plannedSetCount
            if (planned > 0) {
                val done = uiState.completedPlannedSetCount
                val progress by animateFloatAsState(
                    targetValue = done.toFloat() / planned,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "workoutProgress",
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    // A muted track, so 0/6 reads as empty rather than as a full bar.
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Spacer(modifier = Modifier.height(4.dp))
                HdRollingText(
                    text = stringResource(R.string.workout_sets_progress, done, planned),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val logged = uiState.completedSets.count { !it.isWarmup }
                Text(
                    text = pluralStringResource(R.plurals.workout_sets_logged, logged, logged),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The rest countdown. The bar glides continuously instead of jumping once a second, the
 * digits roll, and the last [REST_WARNING_SECONDS] seconds turn it to the accent color with a
 * tick each second so the lifter can get set without watching the screen.
 */
@Composable
private fun RestTimerBanner(
    rest: RestTimerState,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val ending = rest.remainingSeconds in 1..REST_WARNING_SECONDS
    LaunchedEffect(rest.remainingSeconds) {
        if (ending) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
    val containerColor by animateColorAsState(
        if (ending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        label = "restContainer",
    )
    val contentColor by animateColorAsState(
        if (ending) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "restContent",
    )
    val barColor by animateColorAsState(
        if (ending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        label = "restBar",
    )
    // Each one-second step is spread over that second, so the bar moves smoothly.
    val progress by animateFloatAsState(
        targetValue = rest.remainingSeconds.toFloat() / rest.totalSeconds.coerceAtLeast(1),
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "restProgress",
    )

    Surface(color = containerColor, contentColor = contentColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.workout_rest),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    HdRollingText(
                        text = formatDuration(rest.remainingSeconds),
                        style = MaterialTheme.typography.titleLarge.tabular(),
                        fontWeight = FontWeight.Bold,
                        countingDown = true,
                    )
                }
                SmallTonalButton(text = stringResource(R.string.workout_rest_minus), onClick = { onAdjust(-REST_STEP_SECONDS) })
                Spacer(modifier = Modifier.width(6.dp))
                SmallTonalButton(text = stringResource(R.string.workout_rest_plus), onClick = { onAdjust(REST_STEP_SECONDS) })
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.workout_rest_skip), color = contentColor)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = barColor,
                trackColor = contentColor.copy(alpha = 0.15f),
                drawStopIndicator = {},
            )
        }
    }
}

private const val REST_WARNING_SECONDS = 3

/** A compact pill tinted from the surrounding content color, so it follows the banner's color. */
@Composable
private fun SmallTonalButton(text: String, onClick: () -> Unit) {
    val contentColor = LocalContentColor.current
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = contentColor.copy(alpha = 0.12f),
            contentColor = contentColor,
        ),
        modifier = Modifier.height(32.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ExerciseStrip(uiState: ActiveWorkoutUiState, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.currentExerciseIndex) {
        listState.animateScrollToItem(uiState.currentExerciseIndex)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(uiState.routineExercises) { index, exercise ->
            val isSelected = index == uiState.currentExerciseIndex
            val done = uiState.setsFor(exercise.exerciseId).count { !it.isWarmup }
            val isDone = uiState.isDone(exercise)
            val containerColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "stripContainer",
            )
            val contentColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                label = "stripContent",
            )
            Surface(
                onClick = { onSelect(index) },
                shape = MaterialTheme.shapes.small,
                color = containerColor,
                contentColor = contentColor,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = exercise.exercise?.name.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The check pops in when the last planned set is logged.
                        AnimatedVisibility(
                            visible = isDone,
                            enter = expandHorizontally() +
                                scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) +
                                fadeIn(),
                            exit = shrinkHorizontally() + fadeOut(),
                        ) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.workout_done),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        HdRollingText(
                            text = if (exercise.targetSets > 0) "$done/${exercise.targetSets}" else "$done",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentExerciseHeader(
    routineExercise: RoutineExercise,
    index: Int,
    count: Int,
    isFreeWorkout: Boolean,
    onSelect: (Int) -> Unit,
) {
    val exercise = routineExercise.exercise
    val unit = routineExercise.displayUnit
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(index - 1) }, enabled = index > 0) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.workout_previous_exercise_cd),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = exercise?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                textAlign = TextAlign.Center,
            )
        }
        IconButton(onClick = { onSelect(index + 1) }, enabled = index < count - 1) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.workout_next_exercise_cd),
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: Prescription,
    weightUnit: WeightUnit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            durationSecs != null -> append("${durationSecs} s")
            weightKg != null -> append("${suggestion.reps} · ${weightKg.kgToRounded(weightUnit).formatDecimal()} ${weightUnit.label}")
            else -> append("${suggestion.reps}")
        }
    }
    Surface(color = containerColor, contentColor = contentColor, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.workout_suggestion, target) +
                        if (suggestion.isDeload) " · ${stringResource(R.string.workout_deload)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
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

// ============================================================== set table

private val SetColumnWidth = 44.dp
private val ValueColumnWidth = 76.dp
private val ActionColumnWidth = 48.dp

@Composable
private fun SetTable(
    uiState: ActiveWorkoutUiState,
    routineExercise: RoutineExercise,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
    modifier: Modifier = Modifier,
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
                modifier = Modifier.width(ValueColumnWidth),
            )
            HeaderCell(
                stringResource(if (uiState.isTimedInput) R.string.workout_col_time else R.string.workout_col_reps),
                Modifier.width(ValueColumnWidth),
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
                    onDelete = { onEvent(ActiveWorkoutUiEvent.OnDeleteSet(set)) },
                )
            }
        }

        ActiveSetRow(
            uiState = uiState,
            setNumber = currentSetNumber,
            previous = uiState.previousSets[currentSetNumber],
            unit = unit,
            onEvent = onEvent,
        )

        // Planned sets still ahead, so the lifter sees what's left of this exercise.
        for (setNumber in (currentSetNumber + 1)..routineExercise.targetSets) {
            PendingSetRow(setNumber = setNumber, previous = uiState.previousSets[setNumber], unit = unit)
        }
    }
}

@Composable
private fun TableRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun ValueCell(text: String, modifier: Modifier, dim: Boolean = false, bold: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.tabular(),
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun SetNumberCell(label: String, highlighted: Boolean) {
    Box(modifier = Modifier.width(SetColumnWidth), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * A logged set. Swipe it left to delete (the trash button does the same); a set logged just
 * now drops into place with a light bounce and a confirming vibration.
 */
@Composable
private fun LoggedSetRow(
    set: WorkoutSet,
    previous: WorkoutSet?,
    unit: WeightUnit,
    animateIn: Boolean,
    onDelete: () -> Unit,
) {
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
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
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
        },
    ) {
        LoggedSetRowContent(set = set, previous = previous, unit = unit, onDelete = onDelete)
    }
}

@Composable
private fun LoggedSetRowContent(
    set: WorkoutSet,
    previous: WorkoutSet?,
    unit: WeightUnit,
    onDelete: () -> Unit,
) {
    TableRow(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            // Opaque, so the delete background doesn't tint the row while it's swiped.
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    .compositeOver(MaterialTheme.colorScheme.surfaceContainerLow),
            ),
    ) {
        SetNumberCell(
            label = if (set.isWarmup) stringResource(R.string.workout_warmup_short) else set.setNumber.toString(),
            highlighted = false,
        )
        ValueCell(previous.describe(unit), Modifier.weight(1f), dim = true)
        // Each set shows in the unit it was logged in — a lb machine set stays "50 lb".
        val setUnit = set.weightUnit ?: unit
        ValueCell(
            text = set.weight?.kgToRounded(setUnit)?.let { "${it.formatDecimal()}${if (setUnit != unit) " ${setUnit.label}" else ""}" } ?: "—",
            modifier = Modifier.width(ValueColumnWidth),
            bold = true,
        )
        val effortSuffix = when (val effort = set.effort) {
            is Effort.Rpe -> " @${effort.value.formatDecimal()}"
            is Effort.Rir -> " (${effort.value})"
            null -> ""
        }
        ValueCell(
            text = (set.durationSecs?.let { "${it} s" } ?: set.reps?.toString() ?: "—") + effortSuffix,
            modifier = Modifier.width(ValueColumnWidth),
            bold = true,
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(ActionColumnWidth)) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = stringResource(R.string.workout_delete_set),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
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
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
) {
    val hint = uiState.currentSetHint
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(vertical = 4.dp),
    ) {
        TableRow {
            SetNumberCell(
                label = if (uiState.isWarmupInput) stringResource(R.string.workout_warmup_short) else setNumber.toString(),
                highlighted = true,
            )
            ValueCell(previous.describe(unit), Modifier.weight(1f), dim = true)
            CompactNumberInput(
                value = uiState.weightInput,
                onValueChange = { onEvent(ActiveWorkoutUiEvent.OnWeightChange(it)) },
                placeholder = hint.weightKg?.kgToRounded(unit)?.formatDecimal() ?: "0",
                keyboardType = KeyboardType.Decimal,
                width = ValueColumnWidth,
            )
            if (uiState.isTimedInput) {
                HoldTimerCell(
                    elapsedSeconds = uiState.workTimerElapsedSecs,
                    placeholderSeconds = hint.durationSecs,
                    isRunning = uiState.isWorkTimerRunning,
                    onToggle = { onEvent(ActiveWorkoutUiEvent.OnToggleWorkTimer) },
                )
            } else {
                CompactNumberInput(
                    value = uiState.repsInput,
                    onValueChange = { onEvent(ActiveWorkoutUiEvent.OnRepsChange(it)) },
                    placeholder = hint.reps?.toString() ?: "0",
                    keyboardType = KeyboardType.Number,
                    width = ValueColumnWidth,
                )
            }
            // Squeezes and springs back on tap; the new row's arrival confirms the set.
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
                    onEvent(ActiveWorkoutUiEvent.OnLogSet)
                },
                enabled = !uiState.isLoggingSet,
                modifier = Modifier
                    .size(ActionColumnWidth)
                    .graphicsLayer {
                        scaleX = press.value
                        scaleY = press.value
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.workout_log_set),
                )
            }
        }
        if (uiState.effortTrackingEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SetColumnWidth + 6.dp, top = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.preferredEffortScale,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                CompactNumberInput(
                    value = uiState.effortInput,
                    onValueChange = { onEvent(ActiveWorkoutUiEvent.OnEffortInputChange(it)) },
                    placeholder = "—",
                    keyboardType = KeyboardType.Decimal,
                    width = 64.dp,
                )
            }
        }
    }
}

@Composable
private fun PendingSetRow(setNumber: Int, previous: WorkoutSet?, unit: WeightUnit) {
    TableRow {
        SetNumberCell(label = setNumber.toString(), highlighted = false)
        ValueCell(previous.describe(unit), Modifier.weight(1f), dim = true)
        ValueCell("—", Modifier.width(ValueColumnWidth), dim = true)
        ValueCell("—", Modifier.width(ValueColumnWidth), dim = true)
        Spacer(modifier = Modifier.width(ActionColumnWidth))
    }
}

@Composable
private fun CompactNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    width: Dp,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.tabular().copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
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
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun HoldTimerCell(
    elapsedSeconds: Int,
    placeholderSeconds: Int?,
    isRunning: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(ValueColumnWidth)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = stringResource(if (isRunning) R.string.workout_hold_stop else R.string.workout_hold_start),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
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
            },
        )
    }
}

@Composable
private fun SetOptions(
    uiState: ActiveWorkoutUiState,
    routineExercise: RoutineExercise,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPlateCalculator by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HdChip(
            text = stringResource(R.string.workout_warmup),
            isSelected = uiState.isWarmupInput,
            onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleWarmupInput) },
        )
        HdChip(
            text = stringResource(R.string.workout_timed),
            isSelected = uiState.isTimedInput,
            onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleTimedInput) },
        )
        if (routineExercise.exercise?.isBodyweight != true) {
            HdChip(
                text = stringResource(R.string.workout_plates),
                isSelected = false,
                onClick = { showPlateCalculator = true },
            )
        }
    }
    if (showPlateCalculator) {
        val unit = uiState.currentUnit
        val initial = uiState.weightInput.toFloatOrNull() ?: uiState.currentSetHint.weightKg?.kgToRounded(unit)
        PlateCalculatorDialog(
            unit = unit,
            initialWeight = initial?.formatDecimal().orEmpty(),
            onDismiss = { showPlateCalculator = false },
        )
    }
}

// ============================================================== dialogs & pickers

@Composable
private fun FinishWorkoutDialog(
    uiState: ActiveWorkoutUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
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
        },
    )
}

@Composable
private fun ExercisePicker(
    query: String,
    results: List<Exercise>,
    onQueryChange: (String) -> Unit,
    onExercisePick: (Exercise) -> Unit,
) {
    HdCard {
        Text(
            text = stringResource(R.string.workout_add_exercise),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HdSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.workout_search_exercises),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        ) {
            items(items = results, key = { it.id }) { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExercisePick(exercise) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HdExerciseThumbnail(exercise = exercise, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(exercise.muscleGroup.nameResId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Plates per side for a target load, in the unit of the equipment (kg or lb plates and bars). */
@Composable
private fun PlateCalculatorDialog(unit: WeightUnit, initialWeight: String, onDismiss: () -> Unit) {
    var targetInput by remember { mutableStateOf(initialWeight) }
    var barWeight by remember { mutableStateOf(unit.barPresets().first()) }

    Dialog(onDismissRequest = onDismiss) {
        HdCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.workout_plate_calculator),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
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
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.workout_plate_bar),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unit.barPresets().forEach { bar ->
                    HdChip(
                        text = "${bar.formatDecimal()} ${unit.label}",
                        isSelected = barWeight == bar,
                        onClick = { barWeight = bar },
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            HdButton(text = stringResource(R.string.workout_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ============================================================== helpers

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}

private const val REST_STEP_SECONDS = 15

/** Header of the weight column; tapping it switches the exercise between kg and lb. */
@Composable
private fun UnitToggleHeader(label: String, onToggle: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = stringResource(R.string.workout_toggle_unit),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(14.dp),
        )
    }
}

/** Tabular (fixed-width) digits so ticking clocks and number columns don't jitter. */
private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** How a previous set reads in the "Anterior" column: "8 × 60", "45 s" or "—". */
private fun WorkoutSet?.describe(unit: WeightUnit): String {
    if (this == null) return "—"
    val setUnit = weightUnit ?: unit
    val weightPart = weight?.takeIf { it > 0f }?.kgToRounded(setUnit)?.formatDecimal()
        ?.let { if (setUnit != unit) "$it ${setUnit.label}" else it }
    return when {
        durationSecs != null -> "${durationSecs} s" + (weightPart?.let { " × $it" } ?: "")
        reps != null && weightPart != null -> "$reps × $weightPart"
        reps != null -> "$reps"
        else -> "—"
    }
}

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
