package com.example.heavyduty.feature.history.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdGradientCard
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.theme.GradientLimeEnd
import com.example.heavyduty.core.ui.theme.GradientLimeStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryListScreen(
    onSessionClick: (Long) -> Unit,
    onExerciseProgressClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HistoryListUiEffect.NavigateToProgress -> onExerciseProgressClick(effect.exerciseId)
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
            text = "History",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats card
        if (!uiState.isLoading && uiState.stats.totalSessions > 0) {
            HdGradientCard(
                gradientColors = listOf(GradientLimeStart, GradientLimeEnd),
            ) {
                Text(
                    text = "Your Stats",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatItem(label = "Sessions", value = "${uiState.stats.completedSessions}")
                    StatItem(label = "Total Time", value = "${uiState.stats.totalDurationMinutes}min")
                    StatItem(label = "Avg Duration", value = "${uiState.stats.averageDurationMinutes}min")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when {
            uiState.isLoading -> {
                HdLoadingIndicator(message = "Loading history...")
            }
            uiState.sessions.isEmpty() -> {
                HdEmptyState(
                    icon = Icons.Default.History,
                    title = "No workouts yet",
                    description = "Complete a workout to see your history",
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        items = uiState.sessions,
                        key = { it.id },
                    ) { session ->
                        HistorySessionItem(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionItem(
    session: WorkoutSession,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.sessionName.ifBlank { "Workout" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                HdChip(
                    text = when (session.status) {
                        WorkoutStatus.COMPLETED -> "Completed"
                        WorkoutStatus.IN_PROGRESS -> "In Progress"
                        WorkoutStatus.CANCELLED -> "Cancelled"
                    },
                    isSelected = session.status == WorkoutStatus.COMPLETED,
                    onClick = { },
                )
                if (session.totalDurationSecs > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${session.totalDurationSecs / 60}min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
