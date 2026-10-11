package com.example.heavyduty.feature.exercises.presentation.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCardListSkeleton
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdExerciseAnimation
import com.example.heavyduty.core.ui.components.HdExerciseThumbnail
import com.example.heavyduty.core.ui.components.HdSearchBar
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.equipmentLabel
import com.example.heavyduty.core.ui.hdSharedBounds
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.feature.exercises.presentation.accentColor
import com.example.heavyduty.feature.exercises.presentation.exerciseImageKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onExerciseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    onConfirmSelection: (List<Long>) -> Unit = {},
    onCancelSelection: () -> Unit = {},
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnExerciseClick by rememberUpdatedState(onExerciseClick)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ExerciseListUiEffect.NavigateToDetail -> currentOnExerciseClick(effect.exerciseId)
                is ExerciseListUiEffect.ShowError -> { /* Could show snackbar */ }
            }
        }
    }

    uiState.previewExercise?.let { exercise ->
        ExercisePreviewDialog(
            exercise = exercise,
            selectionMode = selectionMode,
            isSelected = exercise.id in uiState.selectedIds,
            onToggleSelection = { viewModel.onEvent(ExerciseListUiEvent.OnToggleSelection(exercise.id)) },
            onDismiss = { viewModel.onEvent(ExerciseListUiEvent.OnPreviewExercise(null)) }
        )
    }

    if (selectionMode) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.selectedIds.isEmpty()) {
                                stringResource(R.string.exercises_pick_title)
                            } else {
                                val count = uiState.selectedIds.size
                                pluralStringResource(R.plurals.exercises_selected_count, count, count)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancelSelection) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_cancel)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (uiState.selectedIds.isNotEmpty()) {
                    Surface(shadowElevation = 8.dp) {
                        HdButton(
                            text = pluralStringResource(
                                R.plurals.exercises_add_to_routine,
                                uiState.selectedIds.size,
                                uiState.selectedIds.size
                            ),
                            onClick = { onConfirmSelection(uiState.selectedIds.toList()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            ExerciseListContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                selectionMode = true,
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else {
        ExerciseListContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            selectionMode = false,
            modifier = modifier
        )
    }
}

@Composable
private fun ExerciseListContent(
    uiState: ExerciseListUiState,
    onEvent: (ExerciseListUiEvent) -> Unit,
    selectionMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (!selectionMode) {
            Text(
                text = stringResource(R.string.exercises_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        HdSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { onEvent(ExerciseListUiEvent.OnSearchQueryChange(it)) },
            placeholder = stringResource(R.string.exercises_search_hint)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Muscle group filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(MuscleGroup.entries.toList()) { group ->
                HdChip(
                    text = stringResource(group.nameResId),
                    isSelected = uiState.selectedFilter == group,
                    onClick = { onEvent(ExerciseListUiEvent.OnFilterSelect(group)) }
                )
            }
        }

        if (uiState.availableEquipment.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.availableEquipment) { equipment ->
                    HdChip(
                        text = equipmentLabel(equipment),
                        isSelected = uiState.selectedEquipment == equipment,
                        onClick = { onEvent(ExerciseListUiEvent.OnEquipmentFilterSelect(equipment)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (!uiState.isLoading) {
            Text(
                text = pluralStringResource(R.plurals.exercises_count, uiState.exercises.size, uiState.exercises.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            uiState.isLoading -> {
                // Same shape as the rows that are coming, so nothing jumps when they arrive.
                HdCardListSkeleton(
                    modifier = Modifier.weight(1f),
                    rows = 5,
                    thumbnailSize = if (selectionMode) 64.dp else 88.dp
                )
            }

            uiState.loadFailed -> HdErrorState(modifier = Modifier.weight(1f))

            uiState.exercises.isEmpty() -> {
                val hasActiveFilters = uiState.searchQuery.isNotBlank() ||
                    uiState.selectedFilter != null || uiState.selectedEquipment != null
                HdEmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = stringResource(R.string.exercises_empty_title),
                    description = if (uiState.searchQuery.isNotBlank()) {
                        stringResource(R.string.exercises_empty_search)
                    } else {
                        stringResource(R.string.exercises_empty_default)
                    },
                    modifier = Modifier.weight(1f),
                    action = if (hasActiveFilters) {
                        {
                            HdTonalButton(
                                text = stringResource(R.string.exercises_clear_filters),
                                onClick = {
                                    onEvent(ExerciseListUiEvent.OnSearchQueryChange(""))
                                    onEvent(ExerciseListUiEvent.OnFilterSelect(null))
                                    onEvent(ExerciseListUiEvent.OnEquipmentFilterSelect(null))
                                }
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            else -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.weight(1f)
                ) {
                    val listState = rememberLazyListState()
                    // New results start at the top, not wherever the previous ones were scrolled to.
                    // Keyed on the results themselves: keying on the query scrolled before the new
                    // results arrived, and the list then kept its position by item key.
                    LaunchedEffect(uiState.exercises) {
                        listState.scrollToItem(0)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(if (selectionMode) 6.dp else 8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = uiState.exercises,
                            key = { it.id }
                        ) { exercise ->
                            if (selectionMode) {
                                val order = uiState.selectedIds.indexOf(exercise.id)
                                SelectableExerciseRow(
                                    exercise = exercise,
                                    selectionOrder = if (order >= 0) order + 1 else null,
                                    onToggle = { onEvent(ExerciseListUiEvent.OnToggleSelection(exercise.id)) },
                                    onPreview = { onEvent(ExerciseListUiEvent.OnPreviewExercise(exercise)) }
                                )
                                return@items
                            }
                            ExerciseListItem(
                                exercise = exercise,
                                selectionMode = selectionMode,
                                isSelected = exercise.id in uiState.selectedIds,
                                onClick = {
                                    if (selectionMode) {
                                        onEvent(ExerciseListUiEvent.OnToggleSelection(exercise.id))
                                    } else {
                                        onEvent(ExerciseListUiEvent.OnExerciseClick(exercise.id))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val accent = exercise.muscleGroup.accentColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            // A thin accent outline instead of a colored drop shadow: on the dark theme the
            // shadow was barely visible, yet each card drew its own shadow layer, one of the
            // most expensive parts of scrolling this 1,300-item list.
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accent else accent.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    // Grows into the detail screen's image when the exercise is opened.
                    .hdSharedBounds(exerciseImageKey(exercise.id), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.25f), accent.copy(alpha = 0.08f))
                        )
                    )
            ) {
                if (exercise.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = exercise.imageUrl,
                        contentDescription = exercise.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }
                if (selectionMode && isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.exercises_selected),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HdChip(
                        text = stringResource(exercise.muscleGroup.nameResId),
                        isSelected = true,
                        onClick = {}
                    )
                    HdChip(
                        text = stringResource(exercise.exerciseType.nameResId),
                        isSelected = false,
                        onClick = {}
                    )
                }
                if (exercise.equipment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = equipmentLabel(exercise.equipment),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact picker row: thumbnail, name, muscle and equipment, and the pick order (1, 2, 3…)
 * once selected — the order the exercises will be added to the routine in.
 */
@Composable
private fun SelectableExerciseRow(exercise: Exercise, selectionOrder: Int?, onToggle: () -> Unit, onPreview: () -> Unit) {
    val accent = exercise.muscleGroup.accentColor()
    val isSelected = selectionOrder != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (isSelected) Modifier.border(2.dp, accent, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onToggle)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HdExerciseThumbnail(exercise = exercise, size = 64.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.displayName.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = listOfNotNull(
                    stringResource(exercise.muscleGroup.nameResId),
                    exercise.equipment.takeIf { it.isNotBlank() }?.let { equipmentLabel(it) }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onPreview) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.exercises_view),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isSelected) accent else Color.Transparent)
                .border(2.dp, if (isSelected) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            if (selectionOrder != null) {
                Text(
                    text = selectionOrder.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/** Animated demo of an exercise, with a select button when picking for a routine. */
@Composable
private fun ExercisePreviewDialog(
    exercise: Exercise,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = exercise.displayName.replaceFirstChar { it.uppercase() },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                HdExerciseAnimation(
                    exercise = exercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = listOfNotNull(
                        stringResource(exercise.muscleGroup.nameResId),
                        exercise.equipment.takeIf { it.isNotBlank() }?.let { equipmentLabel(it) }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (selectionMode) {
                TextButton(
                    onClick = {
                        onToggleSelection()
                        onDismiss()
                    }
                ) { Text(stringResource(if (isSelected) R.string.exercises_unselect else R.string.exercises_select)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
        dismissButton = if (selectionMode) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
        } else {
            null
        }
    )
}
