package com.example.heavyduty.feature.calendar.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.nameResId
import java.time.DayOfWeek
import java.time.format.TextStyle
import kotlinx.coroutines.launch

/** Weekday name in the app's language; [day] is 1 = Monday … 7 = Sunday. */
@Composable
@ReadOnlyComposable
private fun dayName(day: Int, style: TextStyle = TextStyle.SHORT): String =
    DayOfWeek.of(day).getDisplayName(style, LocalConfiguration.current.locales[0])
        .replaceFirstChar { it.titlecase(LocalConfiguration.current.locales[0]) }

@Composable
fun CalendarScreen(onRoutineClick: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    val currentOnRoutineClick by rememberUpdatedState(onRoutineClick)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CalendarUiEffect.NavigateToRoutine -> currentOnRoutineClick(effect.routineId)
                is CalendarUiEffect.ShowMessage -> launch { snackbarHostState.showSnackbar(resources.getString(effect.messageResId)) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    HdCardsSkeleton(modifier = Modifier.padding(16.dp), heights = listOf(56.dp, 96.dp, 96.dp))
                }

                uiState.loadFailed -> HdErrorState()

                else -> {
                    WeekDaySelector(selectedDay = uiState.selectedDay, weekSchedule = uiState.weekSchedule, onEvent = viewModel::onEvent)

                    Spacer(modifier = Modifier.height(20.dp))

                    val selectedDay = uiState.selectedDay
                    if (selectedDay != null) {
                        DaySchedule(day = selectedDay, uiState = uiState, onEvent = viewModel::onEvent)
                    } else {
                        WeekOverview(uiState = uiState)
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

/** Monday to Sunday; a dot marks the days that have a routine. */
@Composable
private fun WeekDaySelector(selectedDay: Int?, weekSchedule: Map<Int, List<CalendarEntry>>, onEvent: (CalendarUiEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (day in 1..7) {
            val isSelected = selectedDay == day
            val hasEntries = weekSchedule[day]?.isNotEmpty() == true

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                    .clickable { onEvent(CalendarUiEvent.OnDaySelect(day)) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = dayName(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                                }
                            )
                    )
                }
            }
        }
    }
}

/** One day: its routines, and the way to assign another. */
@Composable
private fun DaySchedule(day: Int, uiState: CalendarUiState, onEvent: (CalendarUiEvent) -> Unit) {
    Column {
        val entries = uiState.weekSchedule[day] ?: emptyList()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.calendar_schedule_for,
                    // As the language writes it mid-sentence: "miércoles" / "Wednesday".
                    DayOfWeek.of(day).getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            HdTonalButton(
                text = stringResource(R.string.calendar_assign_routine),
                onClick = { onEvent(CalendarUiEvent.OnToggleRoutinePicker) },
                icon = Icons.Default.Add
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isPickingRoutine) {
            RoutinePicker(
                routines = uiState.routines,
                onRoutinePick = {
                    onEvent(CalendarUiEvent.OnAssignRoutine(it.id, day))
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (entries.isEmpty()) {
            HdEmptyState(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.calendar_rest_day),
                description = stringResource(R.string.calendar_rest_day_hint)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    CalendarEntryItem(
                        routineName =
                            uiState.routines.find { it.id == entry.routineId }?.name
                                ?: stringResource(R.string.calendar_routine_fallback, entry.routineId.toInt()),
                        onClick = { onEvent(CalendarUiEvent.OnRoutineClick(entry.routineId)) },
                        onRemove = { onEvent(CalendarUiEvent.OnRemoveEntry(entry)) }
                    )
                }
            }
        }
    }
}

/** No day selected: every day of the week with its routines. */
@Composable
private fun WeekOverview(uiState: CalendarUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items((1..7).toList()) { day ->
            val entries = uiState.weekSchedule[day] ?: emptyList()
            HdCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayName(day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (entries.isEmpty()) {
                            stringResource(
                                R.string.calendar_rest_day
                            )
                        } else {
                            pluralStringResource(R.plurals.calendar_routine_count, entries.size, entries.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entries.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
                entries.forEach { entry ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• ${uiState.routines.find {
                            it.id == entry.routineId
                        }?.name ?: stringResource(R.string.calendar_routine_generic)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEntryItem(routineName: String, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    HdCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = routineName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.calendar_remove),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RoutinePicker(routines: List<Routine>, onRoutinePick: (Routine) -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.calendar_pick_routine),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (routines.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_routines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(routine.type.nameResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
