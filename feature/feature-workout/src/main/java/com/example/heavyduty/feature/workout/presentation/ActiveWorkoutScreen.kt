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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdGradientCard
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
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
                message = "Loading workout...",
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
            text = "Ready to train?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start a workout from a routine or go free-style",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        HdButton(
            text = "Choose a Routine",
            onClick = { onEvent(ActiveWorkoutUiEvent.OnNavigateToRoutines) },
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        HdTonalButton(
            text = "Free Workout",
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
                    text = uiState.sessionName.ifBlank { "Workout" },
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

        // Exercise list from routine
        if (uiState.routineExercises.isNotEmpty()) {
            item {
                Text(
                    text = "Exercises",
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = routineExercise.exercise?.name ?: "Exercise #${routineExercise.exerciseId}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = "${routineExercise.targetSets} sets × ${routineExercise.targetReps} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Log set input
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Log Set #${uiState.currentSetNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.repsInput,
                        onValueChange = { onEvent(ActiveWorkoutUiEvent.OnRepsChange(it)) },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    OutlinedTextField(
                        value = uiState.weightInput,
                        onValueChange = { onEvent(ActiveWorkoutUiEvent.OnWeightChange(it)) },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HdButton(
                    text = "Log Set",
                    onClick = { onEvent(ActiveWorkoutUiEvent.OnLogSet) },
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Complete workout button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HdButton(
                text = "Complete Workout",
                onClick = { onEvent(ActiveWorkoutUiEvent.OnCompleteWorkout) },
                icon = Icons.Default.Stop,
                modifier = Modifier.fillMaxWidth(),
            )
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
