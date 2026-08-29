package com.example.heavyduty.feature.workout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.Prescription
import com.example.heavyduty.core.domain.model.isBodyweight
import com.example.heavyduty.core.domain.model.isPerSide
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdGradientCard
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdSearchBar
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.theme.GradientCyanEnd
import com.example.heavyduty.core.ui.theme.GradientCyanStart

@Composable
fun ActiveWorkoutScreen(
    onNavigateToRoutines: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ActiveWorkoutUiEffect.NavigateToRoutines -> onNavigateToRoutines()
                ActiveWorkoutUiEffect.WorkoutCompleted -> { /* Could show celebration */ }
                is ActiveWorkoutUiEffect.ShowMessage -> { /* Could show snackbar */ }
            }
        }
    }

    when {
        uiState.isLoading -> {
            HdLoadingIndicator(
                modifier = modifier,
                message = "Cargando entrenamiento...",
            )
        }
        uiState.isActive -> {
            ActiveWorkoutContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = modifier,
            )
        }
        else -> {
            WorkoutStartScreen(
                onEvent = viewModel::onEvent,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun WorkoutStartScreen(
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "¿Listo para entrenar?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Empieza un entrenamiento desde una rutina o hazlo libre",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        HdButton(
            text = "Elegir Rutina",
            onClick = { onEvent(ActiveWorkoutUiEvent.OnNavigateToRoutines) },
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        HdTonalButton(
            text = "Entrenamiento Libre",
            onClick = { onEvent(ActiveWorkoutUiEvent.OnStartFreeWorkout) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onEvent: (ActiveWorkoutUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    ) {
        // Timer and session info
        item {
            HdGradientCard(
                gradientColors = listOf(GradientCyanStart, GradientCyanEnd),
            ) {
                Text(
                    text = uiState.sessionName.ifBlank { "Entrenamiento" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatDuration(uiState.elapsedSeconds),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Exercise list (from routine, or added ad-hoc during a free workout)
        if (uiState.routineExercises.isNotEmpty()) {
            item {
                Text(
                    text = "Ejercicios",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            itemsIndexed(uiState.routineExercises) { index, routineExercise ->
                val isSelected = index == uiState.currentExerciseIndex
                HdCard(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEvent(ActiveWorkoutUiEvent.OnSelectExercise(index))
                        },
                ) {
                    Text(
                        text = routineExercise.exercise?.name ?: "Ejercicio #${routineExercise.exerciseId}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = if (routineExercise.exercise?.isPerSide == true) {
                            "${routineExercise.targetSets} sets × ${routineExercise.targetReps} reps " +
                                "(${routineExercise.targetReps / 2} por lado)"
                        } else {
                            "${routineExercise.targetSets} sets × ${routineExercise.targetReps} reps"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.allowAddingExercises) {
                item {
                    HdTextButton(
                        text = "Agregar otro ejercicio",
                        onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleExercisePicker) },
                        icon = Icons.Default.Add,
                    )
                }
            }

            // Log set input
            item {
                val currentExercise = uiState.routineExercises.getOrNull(uiState.currentExerciseIndex)?.exercise
                val isBodyweightExercise = currentExercise?.isBodyweight == true
                val isPerSideExercise = currentExercise?.isPerSide == true
                var showExtraWeight by remember(uiState.currentExerciseIndex) { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registrar Serie #${uiState.currentSetNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                uiState.suggestedPrescription?.let { suggestion ->
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionCard(
                        suggestion = suggestion,
                        onUseSuggestion = { onEvent(ActiveWorkoutUiEvent.OnUseSuggestion) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.repsInput,
                        onValueChange = { onEvent(ActiveWorkoutUiEvent.OnRepsChange(it)) },
                        label = { Text("Repeticiones") },
                        supportingText = if (isPerSideExercise) {
                            {
                                val total = uiState.repsInput.toIntOrNull()
                                Text(if (total != null) "${total / 2} por lado" else "Total ambos lados")
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    if (!isBodyweightExercise || showExtraWeight) {
                        OutlinedTextField(
                            value = uiState.weightInput,
                            onValueChange = { onEvent(ActiveWorkoutUiEvent.OnWeightChange(it)) },
                            label = { Text(if (isBodyweightExercise) "Peso extra (kg)" else "Peso (kg)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    if (uiState.effortTrackingEnabled) {
                        OutlinedTextField(
                            value = uiState.effortInput,
                            onValueChange = { onEvent(ActiveWorkoutUiEvent.OnEffortInputChange(it)) },
                            label = { Text(uiState.preferredEffortScale) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                if (isBodyweightExercise && !showExtraWeight) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HdTextButton(
                        text = "Agregar peso extra (chaleco)",
                        onClick = { showExtraWeight = true },
                        icon = Icons.Default.Add,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HdChip(
                    text = "Calentamiento",
                    isSelected = uiState.isWarmupInput,
                    onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleWarmupInput) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                HdButton(
                    text = "Registrar Serie",
                    onClick = { onEvent(ActiveWorkoutUiEvent.OnLogSet) },
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (uiState.allowAddingExercises) {
            item {
                HdEmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = "Aún no hay ejercicios",
                    description = "Agrega un ejercicio para empezar a registrar series",
                )
                Spacer(modifier = Modifier.height(12.dp))
                HdButton(
                    text = "Agregar Ejercicio",
                    onClick = { onEvent(ActiveWorkoutUiEvent.OnToggleExercisePicker) },
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (uiState.isPickingExercise) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ExercisePicker(
                    query = uiState.exercisePickerQuery,
                    results = uiState.exercisePickerResults,
                    onQueryChange = { onEvent(ActiveWorkoutUiEvent.OnExercisePickerQueryChange(it)) },
                    onExercisePick = { onEvent(ActiveWorkoutUiEvent.OnAddExercise(it)) },
                )
            }
        }

        // Complete workout button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HdButton(
                text = "Finalizar Entrenamiento",
                onClick = { onEvent(ActiveWorkoutUiEvent.OnCompleteWorkout) },
                icon = Icons.Default.Stop,
                modifier = Modifier.fillMaxWidth(),
            )
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
            text = "Agregar Ejercicio",
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
                .height(220.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items = results, key = { it.id }) { exercise ->
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExercisePick(exercise) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: Prescription,
    onUseSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = buildString {
        append(suggestion.sets)
        append(" series × ")
        when {
            suggestion.durationSecs != null -> append("${suggestion.durationSecs}s")
            suggestion.weightKg != null -> append("${suggestion.reps} reps × ${suggestion.weightKg.formatDecimal()}kg")
            else -> append("${suggestion.reps} reps")
        }
    }
    HdCard(
        containerColor = if (suggestion.isDeload) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sugerencia: $target",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (suggestion.isDeload) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
                suggestion.reasonNote?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (suggestion.isDeload) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
            }
            HdTonalButton(text = "Usar", onClick = onUseSuggestion)
        }
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
