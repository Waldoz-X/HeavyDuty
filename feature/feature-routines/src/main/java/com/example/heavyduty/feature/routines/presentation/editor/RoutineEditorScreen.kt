package com.example.heavyduty.feature.routines.presentation.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.WorkoutType
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdSearchBar
import com.example.heavyduty.core.ui.components.HdTonalButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onNavigateBack: () -> Unit,
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
                            onClick = { viewModel.onEvent(RoutineEditorUiEvent.OnToggleExercisePicker) },
                            icon = Icons.Default.Add,
                        )
                    }
                }

                if (uiState.isPickingExercise) {
                    item {
                        ExercisePicker(
                            query = uiState.exercisePickerQuery,
                            results = uiState.exercisePickerResults,
                            onQueryChange = { viewModel.onEvent(RoutineEditorUiEvent.OnExercisePickerQueryChange(it)) },
                            onExercisePick = { viewModel.onEvent(RoutineEditorUiEvent.OnAddExercise(it)) },
                        )
                    }
                }

                // Exercise items
                itemsIndexed(uiState.exercises) { index, routineExercise ->
                    val previous = uiState.exercises.getOrNull(index - 1)
                    val next = uiState.exercises.getOrNull(index + 1)
                    val isInSuperset = routineExercise.supersetGroupId != null &&
                        (routineExercise.supersetGroupId == previous?.supersetGroupId ||
                            routineExercise.supersetGroupId == next?.supersetGroupId)

                    RoutineExerciseItem(
                        routineExercise = routineExercise,
                        isInSuperset = isInSuperset,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.exercises.lastIndex,
                        onUpdate = { updated ->
                            viewModel.onEvent(RoutineEditorUiEvent.OnUpdateExercise(index, updated))
                        },
                        onMoveUp = { viewModel.onEvent(RoutineEditorUiEvent.OnMoveExercise(index, -1)) },
                        onMoveDown = { viewModel.onEvent(RoutineEditorUiEvent.OnMoveExercise(index, 1)) },
                        onRemove = { viewModel.onEvent(RoutineEditorUiEvent.OnRemoveExercise(routineExercise)) },
                    )

                    if (next != null) {
                        val linkedWithNext = routineExercise.supersetGroupId != null &&
                            routineExercise.supersetGroupId == next.supersetGroupId
                        SupersetConnector(
                            isLinked = linkedWithNext,
                            onToggle = {
                                val event = if (linkedWithNext) {
                                    RoutineEditorUiEvent.OnUnpair(index)
                                } else {
                                    RoutineEditorUiEvent.OnPairWithNext(index)
                                }
                                viewModel.onEvent(event)
                            },
                        )
                    }
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

@Composable
private fun ExercisePicker(
    query: String,
    results: List<Exercise>,
    onQueryChange: (String) -> Unit,
    onExercisePick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Buscar Ejercicio",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HdSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Buscar ejercicios...",
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
        ) {
            items(items = results, key = { it.id }) { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExercisePick(exercise) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        if (exercise.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = exercise.imageUrl,
                                contentDescription = exercise.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineExerciseItem(
    routineExercise: RoutineExercise,
    isInSuperset: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUpdate: (RoutineExercise) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(
        containerColor = if (isInSuperset) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
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
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Subir",
                        tint = if (canMoveUp) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bajar",
                        tint = if (canMoveDown) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                    )
                }
            }
            IconButton(onClick = onRemove) {
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
                CompactNumberField(
                    label = "Peso (kg)",
                    value = routineExercise.targetWeight.toInt(),
                    onValueChange = { onUpdate(routineExercise.copy(targetWeight = it.toFloat())) },
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
