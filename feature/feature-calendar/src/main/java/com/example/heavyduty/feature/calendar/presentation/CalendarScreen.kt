package com.example.heavyduty.feature.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.nameResId
import androidx.compose.ui.res.stringResource
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdTonalButton

private val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

@Composable
fun CalendarScreen(
    onRoutineClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CalendarUiEffect.NavigateToRoutine -> onRoutineClick(effect.routineId)
                is CalendarUiEffect.ShowMessage -> { /* Could show snackbar */ }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calendario",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                HdLoadingIndicator(message = "Cargando horario...")
            }
            else -> {
                // Week day selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    (1..7).forEach { day ->
                        val isSelected = uiState.selectedDay == day
                        val hasEntries = uiState.weekSchedule[day]?.isNotEmpty() == true

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                )
                                .clickable { viewModel.onEvent(CalendarUiEvent.OnDaySelect(day)) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = dayNames[day - 1],
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (hasEntries) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.tertiary
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Day content
                val selectedDay = uiState.selectedDay
                if (selectedDay != null) {
                    val entries = uiState.weekSchedule[selectedDay] ?: emptyList()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Horario del ${dayNames[selectedDay - 1]}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        HdTonalButton(
                            text = "Asignar Rutina",
                            onClick = { viewModel.onEvent(CalendarUiEvent.OnToggleRoutinePicker) },
                            icon = Icons.Default.Add,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.isPickingRoutine) {
                        RoutinePicker(
                            routines = uiState.routines,
                            onRoutinePick = {
                                viewModel.onEvent(CalendarUiEvent.OnAssignRoutine(it.id, selectedDay))
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (entries.isEmpty()) {
                        HdEmptyState(
                            icon = Icons.Default.CalendarMonth,
                            title = "Día de descanso",
                            description = "Asigna una rutina para planear este día",
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                            items(entries, key = { it.id }) { entry ->
                                CalendarEntryItem(
                                    routineName = uiState.routines.find { it.id == entry.routineId }?.name ?: "Rutina #${entry.routineId}",
                                    onClick = { viewModel.onEvent(CalendarUiEvent.OnRoutineClick(entry.routineId)) },
                                    onRemove = { viewModel.onEvent(CalendarUiEvent.OnRemoveEntry(entry)) },
                                )
                            }
                        }
                    }
                } else {
                    // Overview — show all days summary
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items((1..7).toList()) { day ->
                            val entries = uiState.weekSchedule[day] ?: emptyList()
                            HdCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = dayNames[day - 1],
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (entries.isEmpty()) "Día de descanso" else "${entries.size} rutina(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (entries.isEmpty()) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.tertiary
                                        },
                                    )
                                }
                                entries.forEach { entry ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• ${uiState.routines.find { it.id == entry.routineId }?.name ?: "Rutina"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEntryItem(
    routineName: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    HdCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = routineName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RoutinePicker(
    routines: List<Routine>,
    onRoutinePick: (Routine) -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Elige una Rutina",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (routines.isEmpty()) {
            Text(
                text = "Aún no tienes rutinas creadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                routines.forEachIndexed { index, routine ->
                    if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onRoutinePick(routine) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(routine.type.nameResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
