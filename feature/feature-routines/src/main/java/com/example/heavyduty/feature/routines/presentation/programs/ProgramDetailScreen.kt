package com.example.heavyduty.feature.routines.presentation.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.domain.model.ProgramWeek
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.HdTonalButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    onNavigateBack: () -> Unit,
    onRoutineClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val program = uiState.program

    uiState.addDayTargetWeekId?.let { weekId ->
        AddTrainingDayDialog(
            availableRoutines = uiState.availableRoutines,
            onDismiss = { viewModel.dismissAddDayDialog() },
            onCreateNew = { name -> viewModel.addNewTrainingDay(weekId, name) },
            onUseExisting = { routine -> viewModel.assignExistingRoutine(weekId, routine) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program?.name ?: "Detalle del programa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading || program == null) {
            HdLoadingIndicator(modifier = Modifier.padding(innerPadding), message = "Cargando programa...")
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(program.weeks, key = { it.id }) { week ->
                    WeekCard(
                        week = week,
                        onRoutineClick = onRoutineClick,
                        onAddDay = { viewModel.openAddDayDialog(week.id) },
                        onRemoveDay = { routine -> viewModel.removeTrainingDay(routine) },
                    )
                }
                item {
                    HdTonalButton(
                        text = "Agregar semana",
                        onClick = { viewModel.addWeek() },
                        icon = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun WeekCard(
    week: ProgramWeek,
    onRoutineClick: (Long) -> Unit,
    onAddDay: () -> Unit,
    onRemoveDay: (Routine) -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Semana ${week.weekNumber}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (week.days.isEmpty()) {
            Text(
                text = "Todavía no hay rutinas asignadas a esta semana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        } else {
            week.days.forEach { routine ->
                TrainingDayRow(
                    routine = routine,
                    onClick = { onRoutineClick(routine.id) },
                    onRemove = { onRemoveDay(routine) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        HdTextButton(
            text = "Agregar día de entrenamiento",
            onClick = onAddDay,
            icon = Icons.Default.Add,
        )
    }
}

@Composable
private fun TrainingDayRow(
    routine: Routine,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = routine.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quitar de la semana",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AddTrainingDayDialog(
    availableRoutines: List<Routine>,
    onDismiss: () -> Unit,
    onCreateNew: (String) -> Unit,
    onUseExisting: (Routine) -> Unit,
) {
    var newRoutineName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar día de entrenamiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (availableRoutines.isNotEmpty()) {
                    Text(
                        text = "Reutilizar una rutina existente",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableRoutines.forEach { routine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onUseExisting(routine) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = routine.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "O crear una rutina nueva",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = newRoutineName,
                    onValueChange = { newRoutineName = it },
                    label = { Text("Nombre de la rutina") },
                    placeholder = { Text("Ej. Empuje, Tirón, Piernas...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreateNew(newRoutineName.trim()) },
                enabled = newRoutineName.isNotBlank(),
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
