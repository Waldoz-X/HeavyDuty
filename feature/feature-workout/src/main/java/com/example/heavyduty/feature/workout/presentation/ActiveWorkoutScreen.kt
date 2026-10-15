package com.example.heavyduty.feature.workout.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.repository.RestTimerState
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.HdTonalButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    onNavigateToRoutines: () -> Unit,
    onWorkoutComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var personalRecord by remember { mutableStateOf<ActiveWorkoutUiEffect.PersonalRecord?>(null) }

    val currentOnNavigateToRoutines by rememberUpdatedState(onNavigateToRoutines)
    val currentOnWorkoutComplete by rememberUpdatedState(onWorkoutComplete)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ActiveWorkoutUiEffect.NavigateToRoutines -> currentOnNavigateToRoutines()

                // Either way the workout is over: back to the dashboard.
                ActiveWorkoutUiEffect.WorkoutCompleted, ActiveWorkoutUiEffect.WorkoutDiscarded -> currentOnWorkoutComplete()

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
                        duration = SnackbarDuration.Long
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

    WorkoutSessionSideEffects(isActive = uiState.isActive)

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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/** What a running workout asks of the phone: notifications for the rest countdown, and the screen on. */
@Composable
private fun WorkoutSessionSideEffects(isActive: Boolean) {
    val context = LocalContext.current
    // The rest countdown lives in a notification while the phone is pocketed or in another
    // app; on Android 13+ that needs the permission, so ask once when a workout starts.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive && !askedForNotifications && needsNotificationPermission(context)) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Keep the screen on while a session is active, so a rest period between sets doesn't
    // lock the phone and lose the lifter's place.
    val view = LocalView.current
    DisposableEffect(isActive) {
        view.keepScreenOn = isActive
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Drops in from the top when a set beats the exercise's best, then leaves on its own (or on
 * tap). It overlays the screen instead of pushing it down, so the set table doesn't jump.
 */
@Composable
private fun PersonalRecordBanner(record: ActiveWorkoutUiEffect.PersonalRecord?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // Keeps drawing the last record while the banner animates out.
    var shown by remember { mutableStateOf(record) }
    if (record != null) shown = record
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(record) {
        if (record != null) {
            delay(PR_BANNER_MS)
            currentOnDismiss()
        }
    }
    AnimatedVisibility(
        visible = record != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { -it } +
            fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        val pr = shown ?: return@AnimatedVisibility
        PersonalRecordCard(record = pr, onClick = onDismiss)
    }
}

@Composable
private fun PersonalRecordCard(record: ActiveWorkoutUiEffect.PersonalRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The trophy pops once the banner has landed.
            val trophyScale = remember(record) { Animatable(0.3f) }
            LaunchedEffect(record) {
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
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.workout_pr_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.workout_pr_detail,
                        record.exerciseName,
                        record.estimatedOneRepMaxKg.kgToRounded(record.unit).formatDecimal(),
                        record.unit.label
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private const val PR_BANNER_MS = 3_500L

/** Internal (not private) so UI tests can drive it with a plain state, no ViewModel or Hilt. */
@Composable
internal fun ActiveWorkoutContent(uiState: ActiveWorkoutUiState, onEvent: (ActiveWorkoutUiEvent) -> Unit) {
    var endDialog by rememberSaveable { mutableStateOf<EndWorkoutDialog?>(null) }
    val currentExercise = uiState.currentExercise

    // A phone on its side is too short to pin the header and the exercise strip above the
    // list (only a sliver of the set table was left), so there they scroll with it. The rest
    // banner stays pinned either way: it's brief and the one thing worth seeing at a glance.
    val compactHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } < COMPACT_HEIGHT
    val showExerciseStrip = uiState.routineExercises.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        if (!compactHeight) {
            WorkoutHeader(uiState = uiState, onFinish = { endDialog = EndWorkoutDialog.FINISH }, onDiscard = {
                endDialog =
                    EndWorkoutDialog.DISCARD
            })
        }

        PinnedRestBanner(restTimer = uiState.restTimer, onEvent = onEvent)

        if (!compactHeight && showExerciseStrip) {
            ExerciseStrip(uiState = uiState, onSelect = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(it)) })
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (compactHeight) {
                scrollingHeaderItems(
                    uiState,
                    showExerciseStrip,
                    onFinish = { endDialog = EndWorkoutDialog.FINISH },
                    onDiscard = { endDialog = EndWorkoutDialog.DISCARD },
                    onEvent = onEvent
                )
            }
            if (currentExercise != null) {
                currentExerciseItems(uiState, currentExercise, onEvent)
            } else if (uiState.allowAddingExercises && !uiState.isPickingExercise) {
                item(key = "empty") {
                    HdEmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = stringResource(R.string.workout_no_exercises_title),
                        description = stringResource(R.string.workout_no_exercises_description)
                    )
                }
            }

            if (uiState.allowAddingExercises) {
                addExerciseItem(uiState, onEvent)
            }
        }
    }

    endDialog?.let { dialog ->
        EndWorkoutDialogs(dialog = dialog, uiState = uiState, onDismiss = { endDialog = null }, onEvent = onEvent)
    }
    uiState.unusualWeight?.let { check ->
        UnusualWeightDialog(
            check = check,
            onConfirm = { onEvent(ActiveWorkoutUiEvent.OnConfirmUnusualWeight) },
            onDismiss = { onEvent(ActiveWorkoutUiEvent.OnDismissUnusualWeight) }
        )
    }
}

/** The two ways out of a workout, each confirmed first. */
private enum class EndWorkoutDialog { FINISH, DISCARD }

@Composable
private fun EndWorkoutDialogs(
    dialog: EndWorkoutDialog,
    uiState: ActiveWorkoutUiState,
    onDismiss: () -> Unit,
    onEvent: (ActiveWorkoutUiEvent) -> Unit
) {
    when (dialog) {
        EndWorkoutDialog.FINISH -> {
            val haptic = LocalHapticFeedback.current
            FinishWorkoutDialog(
                uiState = uiState,
                onConfirm = {
                    onDismiss()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(ActiveWorkoutUiEvent.OnCompleteWorkout)
                },
                onDismiss = onDismiss
            )
        }

        EndWorkoutDialog.DISCARD -> DiscardWorkoutDialog(
            uiState = uiState,
            onConfirm = {
                onDismiss()
                onEvent(ActiveWorkoutUiEvent.OnDiscardWorkout)
            },
            onDismiss = onDismiss
        )
    }
}

/** The rest timer, pinned above the list; keeps drawing the last state while it animates out. */
@Composable
private fun PinnedRestBanner(restTimer: RestTimerState?, onEvent: (ActiveWorkoutUiEvent) -> Unit) {
    var lastRest by remember { mutableStateOf(restTimer) }
    if (restTimer != null) lastRest = restTimer
    AnimatedVisibility(
        visible = restTimer != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        lastRest?.let { rest ->
            RestTimerBanner(
                rest = rest,
                onAdjust = { onEvent(ActiveWorkoutUiEvent.OnAdjustRest(it)) },
                onSkip = { onEvent(ActiveWorkoutUiEvent.OnSkipRest) }
            )
        }
    }
}

/** On a short window the workout header and exercise strip scroll with the list, edge to edge. */
private fun LazyListScope.scrollingHeaderItems(
    uiState: ActiveWorkoutUiState,
    showExerciseStrip: Boolean,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onEvent: (ActiveWorkoutUiEvent) -> Unit
) {
    item(key = "workoutHeader") {
        Box(modifier = Modifier.bleedHorizontally(16.dp)) {
            WorkoutHeader(uiState = uiState, onFinish = onFinish, onDiscard = onDiscard)
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

/** The current exercise: header, suggestion, set table, options and the way to the next one. */
private fun LazyListScope.currentExerciseItems(
    uiState: ActiveWorkoutUiState,
    currentExercise: RoutineExercise,
    onEvent: (ActiveWorkoutUiEvent) -> Unit
) {
    // The exercise's own items are keyed by it, so switching exercises fades the old
    // ones out and the new ones in (animateItem) instead of swapping them abruptly.
    val exerciseKey = currentExercise.exerciseId
    item(key = "header") {
        AnimatedExerciseHeader(uiState = uiState, onEvent = onEvent)
    }
    uiState.suggestedPrescription?.let { suggestion ->
        item(key = "suggestion-$exerciseKey") {
            SuggestionRow(
                suggestion = suggestion,
                weightUnit = uiState.currentUnit,
                onUse = { onEvent(ActiveWorkoutUiEvent.OnUseSuggestion) },
                modifier = Modifier.animateItem()
            )
        }
    }
    item(key = "sets-$exerciseKey") {
        SetTable(
            uiState = uiState,
            routineExercise = currentExercise,
            onEvent = onEvent,
            modifier = Modifier.animateItem()
        )
    }
    item(key = "options-$exerciseKey") {
        SetOptions(
            uiState = uiState,
            routineExercise = currentExercise,
            onEvent = onEvent,
            modifier = Modifier.animateItem()
        )
    }
    nextExerciseIndex(uiState)?.let { nextIndex ->
        item(key = "next") {
            val nextName = uiState.routineExercises[nextIndex].exercise?.displayName.orEmpty()
            HdTonalButton(
                text = stringResource(R.string.workout_next_exercise, nextName),
                onClick = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(nextIndex)) },
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            )
        }
    }
}

/** The current exercise's header, sliding the way the lifter moved through the routine. */
@Composable
private fun AnimatedExerciseHeader(uiState: ActiveWorkoutUiState, onEvent: (ActiveWorkoutUiEvent) -> Unit) {
    AnimatedContent(
        targetState = uiState.currentExerciseIndex,
        transitionSpec = {
            // Slides the way the lifter moved through the routine.
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(tween(EXERCISE_SWITCH_MS)) { direction * it / 4 } + fadeIn(tween(EXERCISE_SWITCH_MS)))
                .togetherWith(
                    slideOutHorizontally(tween(EXERCISE_SWITCH_MS)) { -direction * it / 4 } +
                        fadeOut(tween(EXERCISE_SWITCH_MS / 2))
                )
                .using(SizeTransform(clip = false))
        },
        label = "exerciseHeader"
    ) { index ->
        val shownExercise = uiState.routineExercises.getOrNull(index) ?: return@AnimatedContent
        CurrentExerciseHeader(
            routineExercise = shownExercise,
            index = index,
            count = uiState.routineExercises.size,
            isFreeWorkout = uiState.allowAddingExercises,
            onSelect = { onEvent(ActiveWorkoutUiEvent.OnSelectExercise(it)) }
        )
    }
}

/** In a free workout: the exercise picker, or the button that opens it. */
private fun LazyListScope.addExerciseItem(uiState: ActiveWorkoutUiState, onEvent: (ActiveWorkoutUiEvent) -> Unit) {
    item(key = "picker") {
        if (uiState.isPickingExercise) {
            ExercisePicker(
                query = uiState.exercisePickerQuery,
                results = uiState.exercisePickerResults,
                onQueryChange = { onEvent(ActiveWorkoutUiEvent.OnExercisePickerQueryChange(it)) },
                onExercisePick = { onEvent(ActiveWorkoutUiEvent.OnAddExercise(it)) }
            )
        } else {
            HdTextButton(
                text = stringResource(R.string.workout_add_exercise),
                onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleExercisePicker) },
                icon = Icons.Default.Add
            )
        }
    }
}

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

private fun needsNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** The next exercise still short of its planned sets, once the current one has all of them. */
private fun nextExerciseIndex(uiState: ActiveWorkoutUiState): Int? {
    val current = uiState.currentExercise ?: return null
    if (current.targetSets <= 0 || !uiState.isDone(current)) return null
    val exercises = uiState.routineExercises
    return (1 until exercises.size)
        .map { (uiState.currentExerciseIndex + it) % exercises.size }
        .firstOrNull { !uiState.isDone(exercises[it]) }
}

internal fun ActiveWorkoutUiState.isDone(exercise: RoutineExercise): Boolean =
    exercise.targetSets > 0 && setsFor(exercise.exerciseId).count { !it.isWarmup } >= exercise.targetSets

/** Tabular (fixed-width) digits so ticking clocks and number columns don't jitter. */
internal fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
