package com.example.heavyduty.feature.routines.presentation.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutType
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdLineChart
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdTonalButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onNavigateBack: () -> Unit,
    onOpenExercisePicker: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                RoutineEditorUiEffect.NavigateBack -> onNavigateBack()
                is RoutineEditorUiEffect.ShowMessage -> { /* Could show snackbar */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Editar Rutina" else "Nueva Rutina",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdLoadingIndicator(
                modifier = Modifier.padding(innerPadding),
                message = "Cargando rutina...",
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Name field
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onEvent(RoutineEditorUiEvent.OnNameChange(it)) },
                        label = { Text("Nombre de la Rutina") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }

                // Description field
                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onEvent(RoutineEditorUiEvent.OnDescriptionChange(it)) },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }

                // Workout type selector
                item {
                    Text(
                        text = "Tipo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(WorkoutType.entries.toList()) { type ->
                            HdChip(
                                text = stringResource(type.nameResId),
                                isSelected = uiState.type == type,
                                onClick = { viewModel.onEvent(RoutineEditorUiEvent.OnTypeChange(type)) },
                            )
                        }
                    }
                }

                // Volume history — only meaningful once the routine has been trained at least
                // a couple of times, so it's absent both for new (unsaved) routines and ones
                // never actually started yet.
                if (uiState.isEditMode && uiState.volumeHistory.size >= 2) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        HdCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Volumen por sesión",
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

                // Exercises section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Ejercicios (${uiState.exercises.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        HdTonalButton(
                            text = "Agregar",
                            onClick = onOpenExercisePicker,
                            icon = Icons.Default.Add,
                        )
                    }
                }

                // Exercise items — a plain Column (not further LazyColumn items) so the drag
                // handle below can reorder freely without fighting lazy-list virtualization.
                // Routines rarely have more than a couple dozen exercises, so the loss of
                // virtualization here is not a concern.
                item {
                    ReorderableExerciseList(
                        exercises = uiState.exercises,
                        onUpdate = { index, updated ->
                            viewModel.onEvent(RoutineEditorUiEvent.OnUpdateExercise(index, updated))
                        },
                        onReorder = { from, to ->
                            viewModel.onEvent(RoutineEditorUiEvent.OnMoveExercise(from, to - from))
                        },
                        onRemove = { viewModel.onEvent(RoutineEditorUiEvent.OnRemoveExercise(it)) },
                        onPairWithNext = { viewModel.onEvent(RoutineEditorUiEvent.OnPairWithNext(it)) },
                        onUnpair = { viewModel.onEvent(RoutineEditorUiEvent.OnUnpair(it)) },
                    )
                }

                // Save button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HdButton(
                        text = if (uiState.isEditMode) "Actualizar Rutina" else "Crear Rutina",
                        onClick = { viewModel.onEvent(RoutineEditorUiEvent.OnSave) },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = uiState.isSaving,
                    )
                }
            }
        }
    }
}

/**
 * Renders the routine's exercises as a plain (non-lazy) draggable column: long-press the
 * handle on an exercise card and drag it up/down to reorder — each threshold crossed swaps
 * it one position via [onReorder], same as the old up/down-arrow buttons did per tap.
 */
@Composable
private fun ReorderableExerciseList(
    exercises: List<RoutineExercise>,
    onUpdate: (Int, RoutineExercise) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onRemove: (RoutineExercise) -> Unit,
    onPairWithNext: (Int) -> Unit,
    onUnpair: (Int) -> Unit,
) {
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Column {
        exercises.forEachIndexed { index, routineExercise ->
            val previous = exercises.getOrNull(index - 1)
            val next = exercises.getOrNull(index + 1)
            val isInSuperset = routineExercise.supersetGroupId != null &&
                (routineExercise.supersetGroupId == previous?.supersetGroupId ||
                    routineExercise.supersetGroupId == next?.supersetGroupId)
            val isDragging = draggedIndex == index

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                    .onGloballyPositioned { itemHeights[index] = it.size.height },
            ) {
                RoutineExerciseItem(
                    routineExercise = routineExercise,
                    isInSuperset = isInSuperset,
                    isDragging = isDragging,
                    onDragStart = {
                        draggedIndex = index
                        dragOffset = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { deltaY ->
                        dragOffset += deltaY
                        val current = draggedIndex
                        val currentHeight = current?.let { itemHeights[it] }
                        if (current != null && currentHeight != null) {
                            val halfHeight = currentHeight / 2f
                            if (dragOffset > halfHeight && current < exercises.lastIndex) {
                                val targetHeight = itemHeights[current + 1] ?: currentHeight
                                onReorder(current, current + 1)
                                draggedIndex = current + 1
                                dragOffset -= targetHeight.toFloat()
                            } else if (dragOffset < -halfHeight && current > 0) {
                                val targetHeight = itemHeights[current - 1] ?: currentHeight
                                onReorder(current, current - 1)
                                draggedIndex = current - 1
                                dragOffset += targetHeight.toFloat()
                            }
                        }
                    },
                    onDragEnd = {
                        draggedIndex = null
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        draggedIndex = null
                        dragOffset = 0f
                    },
                    onUpdate = { updated -> onUpdate(index, updated) },
                    onRemove = { onRemove(routineExercise) },
                )
            }

            if (next != null) {
                val linkedWithNext = routineExercise.supersetGroupId != null &&
                    routineExercise.supersetGroupId == next.supersetGroupId
                SupersetConnector(
                    isLinked = linkedWithNext,
                    onToggle = {
                        if (linkedWithNext) onUnpair(index) else onPairWithNext(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun RoutineExerciseItem(
    routineExercise: RoutineExercise,
    isInSuperset: Boolean,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onUpdate: (RoutineExercise) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(
        containerColor = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer
            isInSuperset -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier,
    ) {
        if (isInSuperset) {
            Text(
                text = "Superset",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(10.dp),
                    ),
            ) {
                val imageUrl = routineExercise.exercise?.imageUrl.orEmpty()
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = routineExercise.exercise?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = routineExercise.exercise?.name ?: "Ejercicio #${routineExercise.exerciseId}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Arrastrar para reordenar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .pointerInput(routineExercise.exerciseId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                    .padding(8.dp)
                    .size(24.dp),
            )
            val haptic = LocalHapticFeedback.current
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Quitar",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactNumberField(
                label = "Series",
                value = routineExercise.targetSets,
                onValueChange = { onUpdate(routineExercise.copy(targetSets = it)) },
                modifier = Modifier.weight(1f),
            )
            CompactNumberField(
                label = "Reps",
                value = routineExercise.targetReps,
                onValueChange = { onUpdate(routineExercise.copy(targetReps = it)) },
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = routineExercise.exercise?.isBodyweight != true,
                modifier = Modifier.weight(1f),
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                val unit = routineExercise.exercise?.weightUnit ?: WeightUnit.KG
                CompactNumberField(
                    label = if (unit == WeightUnit.LB) "Peso (lb)" else "Peso (kg)",
                    value = routineExercise.targetWeight.kgTo(unit).toInt(),
                    onValueChange = { onUpdate(routineExercise.copy(targetWeight = it.toFloat().toKgFrom(unit))) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Progresión",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            val isTimeBased = routineExercise.progressionPolicy == ProgressionPolicy.TIME
            val rangeLabel = if (isTimeBased) "Seg" else "Reps"
            val showStallThreshold = routineExercise.progressionPolicy == ProgressionPolicy.LINEAR ||
                routineExercise.progressionPolicy == ProgressionPolicy.GREYSKULL_LP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactNumberField(
                    label = "$rangeLabel mín",
                    value = routineExercise.repRangeLow,
                    onValueChange = { onUpdate(routineExercise.copy(repRangeLow = it)) },
                    modifier = Modifier.weight(1f),
                )
                CompactNumberField(
                    label = "$rangeLabel máx",
                    value = routineExercise.repRangeHigh,
                    onValueChange = { onUpdate(routineExercise.copy(repRangeHigh = it)) },
                    modifier = Modifier.weight(1f),
                )
                if (showStallThreshold) {
                    CompactNumberField(
                        label = "Fallos",
                        value = routineExercise.stallThreshold,
                        onValueChange = { onUpdate(routineExercise.copy(stallThreshold = it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SupersetConnector(
    isLinked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HdTonalButton(
            text = if (isLinked) "Separar" else "Unir en superset",
            onClick = onToggle,
            icon = if (isLinked) Icons.Default.LinkOff else Icons.Default.Link,
        )
    }
}

@Composable
private fun CompactNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = if (value > 0) value.toString() else "",
        onValueChange = { text ->
            val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
            onValueChange(parsed.coerceAtMost(999))
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    )
}
