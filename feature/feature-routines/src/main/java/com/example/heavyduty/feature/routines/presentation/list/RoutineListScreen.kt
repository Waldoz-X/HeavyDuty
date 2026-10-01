package com.example.heavyduty.feature.routines.presentation.list

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdExerciseThumbnail
import com.example.heavyduty.core.ui.components.HdSkeletonCard
import com.example.heavyduty.core.ui.nameResId
import kotlinx.coroutines.launch

@Composable
fun RoutineListScreen(
    onRoutineClick: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is RoutineListUiEffect.NavigateToEditor -> {
                    if (effect.routineId != null) onRoutineClick(effect.routineId) else onCreateRoutine()
                }
                is RoutineListUiEffect.StartRoutine -> onStartRoutine(effect.routineId)
                is RoutineListUiEffect.RoutineDeleted -> scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.routine_deleted, effect.name))
                }
                is RoutineListUiEffect.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(context.getString(effect.messageResId))
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onEvent(RoutineListUiEvent.OnCreateRoutine) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.routine_create)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.routine_list_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(3) { HdSkeletonCard() }
                    }
                }
                uiState.routines.isEmpty() -> {
                    HdEmptyState(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = stringResource(R.string.routine_list_empty_title),
                        description = stringResource(R.string.routine_list_empty_description),
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(items = uiState.routines, key = { it.id }) { routine ->
                            RoutineListItem(
                                routine = routine,
                                onClick = { viewModel.onEvent(RoutineListUiEvent.OnRoutineClick(routine.id)) },
                                onStart = { viewModel.onEvent(RoutineListUiEvent.OnStartRoutine(routine.id)) },
                                onDelete = { routineToDelete = routine },
                            )
                        }
                    }
                }
            }
        }
    }

    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text(stringResource(R.string.routine_delete_title, routine.name)) },
            text = { Text(stringResource(R.string.routine_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        routineToDelete = null
                        viewModel.onEvent(RoutineListUiEvent.OnDeleteRoutine(routine))
                    },
                ) { Text(stringResource(R.string.routine_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) { Text(stringResource(R.string.routine_cancel)) }
            },
        )
    }
}

@Composable
private fun RoutineListItem(
    routine: Routine,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    HdCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = buildList {
                    add(stringResource(routine.type.nameResId))
                    if (routine.exercises.isNotEmpty()) {
                        add(pluralStringResource(R.plurals.workout_exercise_count, routine.exercises.size, routine.exercises.size))
                        add(stringResource(R.string.routine_summary, routine.exercises.sumOf { it.targetSets }, estimatedMinutes(routine)))
                    }
                }
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.routine_more_options))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.routine_menu_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onClick() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.routine_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                    )
                }
            }
        }

        if (routine.exercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            // The first exercises at a glance, so routines are recognizable without opening them.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                routine.exercises.take(MAX_THUMBNAILS).forEach { HdExerciseThumbnail(exercise = it.exercise, size = 40.dp) }
                val remaining = routine.exercises.size - MAX_THUMBNAILS
                if (remaining > 0) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }

        if (routine.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = routine.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (routine.exercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HdButton(
                text = stringResource(R.string.routine_start),
                onClick = onStart,
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val MAX_THUMBNAILS = 5

private fun estimatedMinutes(routine: Routine): Int =
    routine.exercises.sumOf { it.targetSets * (40 + it.restBetweenSetsSecs) } / 60
