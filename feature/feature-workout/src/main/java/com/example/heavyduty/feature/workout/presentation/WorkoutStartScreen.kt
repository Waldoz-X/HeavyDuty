package com.example.heavyduty.feature.workout.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.HdTonalButton

// What the workout tab shows before a workout starts: free workout or one of the routines.

@Composable
internal fun WorkoutStartScreen(routines: List<Routine>, onEvent: (ActiveWorkoutUiEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { StartTitle() }
        item { FreeWorkoutCard(onClick = { onEvent(ActiveWorkoutUiEvent.OnStartFreeWorkout) }) }
        item { RoutinesHeader(showManage = routines.isNotEmpty(), onManage = { onEvent(ActiveWorkoutUiEvent.OnNavigateToRoutines) }) }
        if (routines.isEmpty()) {
            item { NoRoutines(onCreate = { onEvent(ActiveWorkoutUiEvent.OnNavigateToRoutines) }) }
        } else {
            items(items = routines, key = { it.id }) { routine ->
                RoutineStartCard(routine = routine, onStart = { onEvent(ActiveWorkoutUiEvent.OnStartRoutine(routine.id)) })
            }
        }
    }
}

@Composable
private fun StartTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.workout_start_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.workout_start_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FreeWorkoutCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.primaryContainer, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.FitnessCenter)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.workout_free_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.workout_free_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RoutinesHeader(showManage: Boolean, onManage: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.workout_your_routines),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (showManage) {
            HdTextButton(text = stringResource(R.string.workout_manage_routines), onClick = onManage)
        }
    }
}

@Composable
private fun NoRoutines(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HdEmptyState(
            icon = Icons.Default.FitnessCenter,
            title = stringResource(R.string.workout_no_routines_title),
            description = stringResource(R.string.workout_no_routines_description)
        )
        Spacer(modifier = Modifier.height(12.dp))
        HdTonalButton(
            text = stringResource(R.string.workout_create_routine),
            onClick = onCreate,
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth()
        )
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
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = when {
                    routine.exercises.isNotEmpty() -> pluralStringResource(
                        R.plurals.workout_exercise_count,
                        routine.exercises.size,
                        routine.exercises.size
                    )

                    else -> routine.description
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            HdButton(
                text = stringResource(R.string.workout_start_action),
                onClick = onStart,
                icon = Icons.Default.PlayArrow
            )
        }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}
