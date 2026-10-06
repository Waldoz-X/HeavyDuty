package com.example.heavyduty.feature.routines.presentation.editor

import androidx.compose.ui.platform.LocalResources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutType
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.domain.model.displayUnit
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.model.loadStep
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdExerciseThumbnail
import com.example.heavyduty.core.ui.components.HdLineChart
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.nameResId
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onNavigateBack: () -> Unit,
    onOpenExercisePicker: () -> Unit,
    modifier: Modifier = Modifier,
    /** This destination's nav-entry handle, where the exercise picker leaves its result. */
    navResults: SavedStateHandle? = null,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val listState = rememberLazyListState()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }

    LaunchedEffect(navResults) {
        navResults?.getStateFlow<LongArray?>(SELECTED_EXERCISE_IDS_KEY, null)?.collect { ids ->
            if (ids != null) {
                navResults[SELECTED_EXERCISE_IDS_KEY] = null
                viewModel.onEvent(RoutineEditorUiEvent.OnExercisesPicked(ids.toList()))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                RoutineEditorUiEffect.NavigateBack -> onNavigateBack()
                is RoutineEditorUiEffect.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageResId))
                }
                is RoutineEditorUiEffect.ShowUndoRemove -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.routine_removed, effect.exerciseName.capitalized()),
                        actionLabel = resources.getString(R.string.routine_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) onEvent(RoutineEditorUiEvent.OnUndoRemove)
                }
                is RoutineEditorUiEffect.ExercisesAdded -> scope.launch {
                    // The new rows are at the end: bring them into view.
                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                    if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                    snackbarHostState.showSnackbar(
                        resources.getQuantityString(R.plurals.routine_exercises_added, effect.count, effect.count),
                    )
                }
            }
        }
    }

    val handleBack = {
        if (uiState.hasUnsavedChanges && !uiState.isSaving) showDiscardDialog = true else onNavigateBack()
    }
    BackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isSaving) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (uiState.isEditMode) R.string.routine_edit else R.string.routine_new),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.routine_back),
                        )
                    }
                },
                actions = {
                    if (uiState.exercises.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showUnitMenu = true }) {
                                Icon(imageVector = Icons.Default.Scale, contentDescription = stringResource(R.string.routine_unit))
                            }
                            DropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.routine_units_all_kg)) },
                                    onClick = {
                                        showUnitMenu = false
                                        onEvent(RoutineEditorUiEvent.OnSetAllUnits(WeightUnit.KG))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.routine_units_all_lb)) },
                                    onClick = {
                                        showUnitMenu = false
                                        onEvent(RoutineEditorUiEvent.OnSetAllUnits(WeightUnit.LB))
                                    },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { onEvent(RoutineEditorUiEvent.OnSave) },
                        enabled = !uiState.isSaving,
                    ) {
                        Text(
                            text = stringResource(R.string.routine_save),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdLoadingIndicator(
                modifier = Modifier.padding(innerPadding),
                message = stringResource(R.string.routine_loading),
            )
        } else {
            EditorContent(
                uiState = uiState,
                onEvent = onEvent,
                onOpenExercisePicker = onOpenExercisePicker,
                listState = listState,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    uiState.editingExercise?.let { exercise ->
        val index = uiState.editingIndex ?: return@let
        ExerciseSettingsSheet(
            routineExercise = exercise,
            onUpdate = { onEvent(RoutineEditorUiEvent.OnUpdateExercise(index, it)) },
            onDismiss = { onEvent(RoutineEditorUiEvent.OnCloseExercise) },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.routine_discard_title)) },
            text = { Text(stringResource(R.string.routine_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onEvent(RoutineEditorUiEvent.OnSave)
                    },
                ) { Text(stringResource(R.string.routine_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onNavigateBack()
                        },
                    ) { Text(stringResource(R.string.routine_discard), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(stringResource(R.string.routine_keep_editing))
                    }
                }
            },
        )
    }
}

@Composable
private fun EditorContent(
    uiState: RoutineEditorUiState,
    onEvent: (RoutineEditorUiEvent) -> Unit,
    onOpenExercisePicker: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val exercises by rememberUpdatedState(uiState.exercises)
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        // Only exercise rows are keyed by id (Long); headers and buttons can't be dragged onto.
        val fromIndex = exercises.indexOfFirst { it.id == from.key }
        val toIndex = exercises.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            onEvent(RoutineEditorUiEvent.OnMoveExercise(fromIndex, toIndex))
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // A new routine starts at its name, keyboard up, so creating one is type-name-and-go.
        if (!uiState.isEditMode && uiState.name.isEmpty()) nameFocus.requestFocus()
    }
    var showDescription by rememberSaveable { mutableStateOf(uiState.description.isNotBlank()) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "name") {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onEvent(RoutineEditorUiEvent.OnNameChange(it)) },
                label = { Text(stringResource(R.string.routine_name_label)) },
                placeholder = { Text(stringResource(R.string.routine_name_placeholder)) },
                isError = uiState.showNameError,
                supportingText = if (uiState.showNameError) {
                    { Text(stringResource(R.string.routine_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
            )
        }

        item(key = "description") {
            if (showDescription) {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { onEvent(RoutineEditorUiEvent.OnDescriptionChange(it)) },
                    label = { Text(stringResource(R.string.routine_description_label)) },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { showDescription = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.routine_add_description))
                }
            }
        }

        item(key = "type") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WorkoutType.entries.toList()) { type ->
                    HdChip(
                        text = stringResource(type.nameResId),
                        isSelected = uiState.type == type,
                        onClick = { onEvent(RoutineEditorUiEvent.OnTypeChange(type)) },
                    )
                }
            }
        }

        // Only meaningful once the routine has been trained a couple of times.
        if (uiState.isEditMode && uiState.volumeHistory.size >= 2) {
            item(key = "volume") {
                HdCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.routine_volume_history),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HdLineChart(
                        data = uiState.volumeHistory,
                        valueLabelFormatter = { "${it.toInt()} kg" },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "exercises_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.routine_exercises_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (uiState.exercises.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.routine_summary, uiState.totalSets, uiState.estimatedMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (uiState.exercises.size > 1) {
                Text(
                    text = stringResource(R.string.routine_reorder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.exercises.isEmpty()) {
            item(key = "empty") {
                HdEmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = stringResource(R.string.routine_empty_title),
                    description = stringResource(R.string.routine_empty_description),
                )
            }
        }

        itemsIndexed(uiState.exercises, key = { _, exercise -> exercise.id }) { index, routineExercise ->
            ReorderableItem(reorderState, key = routineExercise.id) { isDragging ->
                val previous = uiState.exercises.getOrNull(index - 1)
                val next = uiState.exercises.getOrNull(index + 1)
                val groupId = routineExercise.supersetGroupId
                val linkedWithNext = groupId != null && groupId == next?.supersetGroupId
                val inSuperset = groupId != null && (groupId == previous?.supersetGroupId || linkedWithNext)
                ExerciseRow(
                    routineExercise = routineExercise,
                    position = index + 1,
                    isDragging = isDragging,
                    inSuperset = inSuperset,
                    linkedWithNext = linkedWithNext,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.exercises.lastIndex,
                    hasNext = next != null,
                    dragHandle = {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = stringResource(R.string.routine_drag_handle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .draggableHandle(
                                    onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                )
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                        )
                    },
                    onOpen = { onEvent(RoutineEditorUiEvent.OnOpenExercise(index)) },
                    onDuplicate = { onEvent(RoutineEditorUiEvent.OnDuplicateExercise(index)) },
                    onMoveUp = { onEvent(RoutineEditorUiEvent.OnMoveExercise(index, index - 1)) },
                    onMoveDown = { onEvent(RoutineEditorUiEvent.OnMoveExercise(index, index + 1)) },
                    onToggleSuperset = {
                        if (linkedWithNext) {
                            onEvent(RoutineEditorUiEvent.OnUnpair(index))
                        } else {
                            onEvent(RoutineEditorUiEvent.OnPairWithNext(index))
                        }
                    },
                    onRemove = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(RoutineEditorUiEvent.OnRemoveExercise(index))
                    },
                )
            }
        }

        item(key = "add") {
            HdTonalButton(
                text = stringResource(R.string.routine_add_exercises),
                onClick = onOpenExercisePicker,
                icon = Icons.Default.Add,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.ExerciseRow(
    routineExercise: RoutineExercise,
    position: Int,
    isDragging: Boolean,
    inSuperset: Boolean,
    linkedWithNext: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    hasNext: Boolean,
    dragHandle: @Composable sh.calvin.reorderable.ReorderableCollectionItemScope.() -> Unit,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleSuperset: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val exercise = routineExercise.exercise
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.shadow(8.dp, MaterialTheme.shapes.medium) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .longPressDraggableHandle()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Superset members share a colored edge, so the grouping reads at a glance.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(if (inSuperset) MaterialTheme.colorScheme.tertiary else androidx.compose.ui.graphics.Color.Transparent),
            )
            dragHandle()
            HdExerciseThumbnail(exercise = exercise, size = 52.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = "$position. ${exercise?.name.orEmpty().capitalized()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (inSuperset) {
                        Text(
                            text = stringResource(R.string.routine_superset),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = routineExercise.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.routine_more_options))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    MenuItem(Icons.Default.Edit, stringResource(R.string.routine_menu_edit)) { showMenu = false; onOpen() }
                    MenuItem(Icons.Default.ContentCopy, stringResource(R.string.routine_menu_duplicate)) { showMenu = false; onDuplicate() }
                    if (canMoveUp) {
                        MenuItem(Icons.Default.KeyboardArrowUp, stringResource(R.string.routine_menu_move_up)) { showMenu = false; onMoveUp() }
                    }
                    if (canMoveDown) {
                        MenuItem(Icons.Default.KeyboardArrowDown, stringResource(R.string.routine_menu_move_down)) { showMenu = false; onMoveDown() }
                    }
                    if (hasNext) {
                        MenuItem(
                            icon = if (linkedWithNext) Icons.Default.LinkOff else Icons.Default.Link,
                            text = stringResource(if (linkedWithNext) R.string.routine_menu_unpair else R.string.routine_menu_superset),
                        ) { showMenu = false; onToggleSuperset() }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.routine_menu_remove), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

/** "3 × 10 · 60 kg · 90 s" — what the row promises at a glance. */
private fun RoutineExercise.summary(): String {
    val unit = displayUnit
    val work = if (progressionPolicy == ProgressionPolicy.TIME && targetDurationSecs > 0) {
        "$targetSets × ${targetDurationSecs} s"
    } else {
        "$targetSets × $targetReps"
    }
    val weight = targetWeight.takeIf { it > 0f }?.let { " · ${it.kgToRounded(unit).formatDecimal()} ${unit.label}" }.orEmpty()
    return "$work$weight · ${formatRest(restBetweenSetsSecs)}"
}

// ============================================================== exercise settings sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSettingsSheet(
    routineExercise: RoutineExercise,
    onUpdate: (RoutineExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exercise = routineExercise.exercise
    // kg/lb of the machine or plates used for this exercise in this routine.
    val unit = routineExercise.displayUnit
    val isTimeBased = routineExercise.progressionPolicy == ProgressionPolicy.TIME
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HdExerciseThumbnail(exercise = exercise, size = 56.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise?.name.orEmpty().capitalized(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    exercise?.muscleGroup?.let {
                        Text(
                            text = stringResource(it.nameResId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberStepper(
                    label = stringResource(R.string.routine_sets),
                    value = routineExercise.targetSets.toFloat(),
                    step = 1f,
                    range = 1f..20f,
                    onValueChange = { onUpdate(routineExercise.copy(targetSets = it.roundToInt())) },
                    modifier = Modifier.weight(1f),
                )
                if (isTimeBased) {
                    NumberStepper(
                        label = stringResource(R.string.routine_duration),
                        value = routineExercise.targetDurationSecs.toFloat(),
                        step = 5f,
                        range = 5f..600f,
                        onValueChange = { onUpdate(routineExercise.copy(targetDurationSecs = it.roundToInt())) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    NumberStepper(
                        label = stringResource(R.string.routine_reps),
                        value = routineExercise.targetReps.toFloat(),
                        step = 1f,
                        range = 1f..100f,
                        onValueChange = { onUpdate(routineExercise.withTargetReps(it.roundToInt())) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.routine_weight), Modifier.weight(1f))
                    UnitToggle(
                        unit = unit,
                        onUnitChange = { newUnit -> onUpdate(routineExercise.copy(weightUnit = newUnit)) },
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
                    modifier = Modifier.fillMaxWidth(),
                )
                if (exercise?.isBodyweight == true) {
                    Text(
                        text = stringResource(R.string.routine_bodyweight_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.routine_rest))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    REST_PRESETS_SECONDS.forEach { seconds ->
                        HdChip(
                            text = formatRest(seconds),
                            isSelected = routineExercise.restBetweenSetsSecs == seconds,
                            onClick = { onUpdate(routineExercise.copy(restBetweenSetsSecs = seconds)) },
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { showAdvanced = !showAdvanced },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel(stringResource(R.string.routine_advanced))
                            Text(
                                text = stringResource(routineExercise.progressionPolicy.nameResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    onClick = { onUpdate(routineExercise.copy(progressionPolicy = policy)) },
                                )
                            }
                        }
                        if (routineExercise.progressionPolicy != ProgressionPolicy.OFF) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberStepper(
                                    label = stringResource(if (isTimeBased) R.string.routine_time_range_min else R.string.routine_rep_range_min),
                                    value = routineExercise.repRangeLow.toFloat(),
                                    step = if (isTimeBased) 5f else 1f,
                                    range = 1f..routineExercise.repRangeHigh.toFloat(),
                                    onValueChange = { onUpdate(routineExercise.copy(repRangeLow = it.roundToInt())) },
                                    modifier = Modifier.weight(1f),
                                )
                                NumberStepper(
                                    label = stringResource(if (isTimeBased) R.string.routine_time_range_max else R.string.routine_rep_range_max),
                                    value = routineExercise.repRangeHigh.toFloat(),
                                    step = if (isTimeBased) 5f else 1f,
                                    range = routineExercise.repRangeLow.toFloat()..600f,
                                    onValueChange = { onUpdate(routineExercise.copy(repRangeHigh = it.roundToInt())) },
                                    modifier = Modifier.weight(1f),
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
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            HdTonalButton(
                text = stringResource(R.string.routine_done),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
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
        modifier = modifier,
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
                label = { Text(option.label) },
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
    format: ((Float) -> String)? = null,
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
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { stepBy(-step) },
                enabled = value > range.start,
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.routine_decrease))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                val style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFeatureSettings = "tnum",
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
                                imeAction = ImeAction.Done,
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
                                },
                        )
                        if (suffix != null) {
                            Text(
                                text = suffix,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text(text = display(value), style = style)
                }
            }
            FilledTonalIconButton(
                onClick = { stepBy(step) },
                enabled = value < range.endInclusive,
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
        repRangeLow = if (linked) (reps - 2).coerceAtLeast(1) else repRangeLow,
    )
}

private val REST_PRESETS_SECONDS = listOf(60, 90, 120, 180)

private fun formatRest(seconds: Int): String =
    if (seconds < 60) "$seconds s" else "%d:%02d".format(seconds / 60, seconds % 60)


private fun String.capitalized(): String = replaceFirstChar { it.uppercase() }
