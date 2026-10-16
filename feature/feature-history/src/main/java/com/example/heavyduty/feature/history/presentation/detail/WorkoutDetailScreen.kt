package com.example.heavyduty.feature.history.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.ui.LocalWeightUnit
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.formatTotalWeight
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** What the user is changing right now; only one dialog at a time. */
private sealed interface DetailDialog {
    data object Duration : DetailDialog
    data object DeleteWorkout : DetailDialog
    data object Share : DetailDialog
    data class EditSet(val setId: Long) : DetailDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    justFinished: Boolean = false,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectDetailEffects(
        effects = viewModel.uiEffect,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onUndoDeleteSet = { viewModel.onEvent(WorkoutDetailUiEvent.OnUndoDeleteSet(it)) }
    )
    WorkoutDetailScaffold(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        justFinished = justFinished,
        modifier = modifier
    )
}

/** Everything the screen draws, from a plain state: internal so UI tests can drive it without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutDetailScaffold(
    uiState: WorkoutDetailUiState,
    onEvent: (WorkoutDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    justFinished: Boolean = false
) {
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }
    val session = uiState.session
    val title = session?.sessionName?.ifBlank { null } ?: stringResource(R.string.history_workout_default)

    Scaffold(
        topBar = {
            DetailTopBar(
                title = title,
                showActions = session != null,
                onNavigateBack = onNavigateBack,
                onShare = { dialog = DetailDialog.Share },
                onEditDuration = { dialog = DetailDialog.Duration },
                onDelete = { dialog = DetailDialog.DeleteWorkout }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> HdCardsSkeleton(modifier = Modifier.padding(innerPadding).padding(16.dp))

            uiState.loadFailed || session == null -> HdErrorState(modifier = Modifier.padding(innerPadding))

            else -> WorkoutDetailContent(
                uiState = uiState,
                justFinished = justFinished,
                onEditSet = { dialog = DetailDialog.EditSet(it.id) },
                onDeleteSet = { onEvent(WorkoutDetailUiEvent.OnDeleteSet(it)) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    DetailDialogs(dialog = dialog, uiState = uiState, title = title, onDismiss = { dialog = null }, onEvent = onEvent)
}

/** Navigates back when the workout is deleted; messages and the set-deleted undo go to [snackbarHostState]. */
@Composable
private fun CollectDetailEffects(
    effects: Flow<WorkoutDetailUiEffect>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onUndoDeleteSet: (WorkoutSet) -> Unit
) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnUndoDeleteSet by rememberUpdatedState(onUndoDeleteSet)
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                WorkoutDetailUiEffect.WorkoutDeleted -> currentOnNavigateBack()

                is WorkoutDetailUiEffect.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageResId))
                }

                is WorkoutDetailUiEffect.SetDeleted -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.workout_set_deleted),
                        actionLabel = resources.getString(R.string.workout_undo),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) currentOnUndoDeleteSet(effect.set)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String,
    showActions: Boolean,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onEditDuration: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        actions = {
            if (showActions) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.history_share))
                }
                IconButton(onClick = onEditDuration) {
                    Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.history_edit_duration))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.history_delete_workout),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun DetailDialogs(
    dialog: DetailDialog?,
    uiState: WorkoutDetailUiState,
    title: String,
    onDismiss: () -> Unit,
    onEvent: (WorkoutDetailUiEvent) -> Unit
) {
    val session = uiState.session ?: return
    when (dialog) {
        null -> Unit

        DetailDialog.Duration -> NumberDialog(
            title = stringResource(R.string.history_edit_duration),
            label = stringResource(R.string.history_duration_minutes_label),
            initial = (session.totalDurationSecs / SECONDS_PER_MINUTE).toString(),
            onConfirm = { minutes ->
                onDismiss()
                onEvent(WorkoutDetailUiEvent.OnSaveDuration(minutes))
            },
            onDismiss = onDismiss
        )

        DetailDialog.Share -> ShareWorkoutDialog(uiState = uiState, title = title, onDismiss = onDismiss)

        DetailDialog.DeleteWorkout -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.history_delete_workout_title)) },
            text = { Text(stringResource(R.string.history_delete_workout_message, title)) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    onEvent(WorkoutDetailUiEvent.OnDeleteWorkout)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        )

        is DetailDialog.EditSet -> session.sets.firstOrNull { it.id == dialog.setId }?.let { set ->
            EditSetDialog(set = set, onDismiss = onDismiss, onSave = { weight, reps, unit ->
                onDismiss()
                onEvent(WorkoutDetailUiEvent.OnSaveSet(set, weight, reps, unit))
            })
        }
    }
}

@Composable
private fun WorkoutDetailContent(
    uiState: WorkoutDetailUiState,
    justFinished: Boolean,
    onEditSet: (WorkoutSet) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (justFinished) item(key = "finished") { FinishedBanner() }
        item(key = "summary") { SummaryCard(uiState) }
        items(uiState.exercises, key = { it.exerciseId }) { exercise ->
            ExerciseCard(exercise = exercise, onEditSet = onEditSet, onDeleteSet = onDeleteSet)
        }
    }
}

/** Heads the detail when it opens as the summary of the workout just finished. */
@Composable
private fun FinishedBanner(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_workout_finished_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.history_workout_finished_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** When it was, how long it took, and how much was done. */
@Composable
private fun SummaryCard(uiState: WorkoutDetailUiState) {
    val session = uiState.session ?: return
    val locale = LocalConfiguration.current.locales[0]
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = SimpleDateFormat("EEEE d MMM yyyy · HH:mm", locale).format(Date(session.startTime)).replaceFirstChar {
                    it.titlecase(locale)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(
                    label = stringResource(R.string.metric_duration),
                    value = stringResource(R.string.duration_minutes, session.totalDurationSecs / SECONDS_PER_MINUTE),
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    label = stringResource(R.string.history_detail_sets),
                    value = uiState.workingSets.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    label = stringResource(R.string.metric_volume),
                    value = uiState.volumeKg.formatTotalWeight(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCard(exercise: ExerciseSets, onEditSet: (WorkoutSet) -> Unit, onDeleteSet: (WorkoutSet) -> Unit) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            exercise.sets.forEach { set -> SetRow(set = set, onEdit = { onEditSet(set) }, onDelete = { onDeleteSet(set) }) }
        }
    }
}

/** "1  ·  60 kg × 8"; tapping it edits weight and reps (timed and cardio sets can only be deleted). */
@Composable
private fun SetRow(set: WorkoutSet, onEdit: () -> Unit, onDelete: () -> Unit) {
    val editable = set.reps != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (set.isWarmup) stringResource(R.string.workout_warmup_short) else set.setNumber.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (set.isWarmup) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = setSummary(set),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.history_delete_set, set.setNumber),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun setSummary(set: WorkoutSet): String {
    val unit = set.weightUnit ?: LocalWeightUnit.current
    val weight = set.weight?.let { "${it.kgToRounded(unit).formatDecimal()} ${unit.label}" }
    return when {
        set.reps != null && weight != null -> "$weight × ${set.reps}"
        set.reps != null -> "× ${set.reps}"
        set.durationSecs != null -> "${set.durationSecs} s"
        else -> weight.orEmpty()
    }
}

/** Weight in the unit the set was logged in, and reps. */
@Composable
private fun EditSetDialog(set: WorkoutSet, onDismiss: () -> Unit, onSave: (weight: String, reps: String, unit: WeightUnit) -> Unit) {
    val unit = set.weightUnit ?: LocalWeightUnit.current
    var weight by rememberSelectedText(set.weight?.kgToRounded(unit)?.formatDecimal() ?: "0")
    var reps by rememberSelectedText(set.reps?.toString().orEmpty())
    val focus = rememberInitialFocus()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_edit_set_title, set.setNumber)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(stringResource(R.string.history_weight_label, unit.label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus)
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text(stringResource(R.string.history_reps_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(weight.text, reps.text, unit) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun NumberDialog(title: String, label: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSelectedText(initial)
    val focus = rememberInitialFocus()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.focusRequester(focus)
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/**
 * A field's text, fully selected to start with: these dialogs correct a value, so typing should
 * replace it — with the cursor at the start, "60" over "2329" became "602329".
 */
@Composable
private fun rememberSelectedText(initial: String) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
    mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length)))
}

/** Focuses the field it's attached to when the dialog opens, keyboard up and value selected. */
@Composable
private fun rememberInitialFocus(): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    return focus
}

private const val SECONDS_PER_MINUTE = 60
