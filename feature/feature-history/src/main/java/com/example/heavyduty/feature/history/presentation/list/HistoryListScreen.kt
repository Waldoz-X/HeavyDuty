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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdGradientCard
import com.example.heavyduty.core.ui.components.HdSkeletonCard
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
    onStartWorkout: () -> Unit = {},
    viewModel: HistoryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnExerciseProgressClick by rememberUpdatedState(onExerciseProgressClick)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HistoryListUiEffect.NavigateToProgress -> currentOnExerciseProgressClick(effect.exerciseId)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats card
        if (!uiState.isLoading && uiState.stats.totalSessions > 0) {
            HdGradientCard(
                gradientColors = listOf(GradientLimeStart, GradientLimeEnd)
            ) {
                Text(
                    text = stringResource(R.string.history_your_stats),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = stringResource(R.string.history_sessions), value = "${uiState.stats.completedSessions}")
                    StatItem(
                        label = stringResource(R.string.history_total_time),
                        value = stringResource(R.string.duration_minutes, uiState.stats.totalDurationMinutes)
                    )
                    StatItem(
                        label = stringResource(R.string.history_avg_duration),
                        value = stringResource(R.string.duration_minutes, uiState.stats.averageDurationMinutes)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when {
            uiState.isLoading -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { HdSkeletonCard() }
                }
            }

            uiState.loadFailed -> HdErrorState()

            uiState.sessions.isEmpty() -> {
                HdEmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.history_empty_title),
                    description = stringResource(R.string.history_empty_description),
                    action = {
                        HdButton(
                            text = stringResource(R.string.history_start_training),
                            onClick = onStartWorkout,
                            icon = Icons.Default.PlayArrow
                        )
                    }
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = uiState.sessions,
                        key = { it.id }
                    ) { session ->
                        HistorySessionItem(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionItem(session: WorkoutSession, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.sessionName.ifBlank { stringResource(R.string.history_workout_default) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(session.startTime, LocalConfiguration.current.locales[0]),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                HdChip(
                    text = when (session.status) {
                        WorkoutStatus.COMPLETED -> stringResource(R.string.status_completed)
                        WorkoutStatus.IN_PROGRESS -> stringResource(R.string.status_in_progress)
                        WorkoutStatus.CANCELLED -> stringResource(R.string.status_cancelled)
                    },
                    isSelected = session.status == WorkoutStatus.COMPLETED,
                    onClick = { }
                )
                if (session.totalDurationSecs > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.duration_minutes, session.totalDurationSecs / 60),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long, locale: Locale): String {
    val sdf = SimpleDateFormat("dd MMM yyyy · HH:mm", locale)
    return sdf.format(Date(timestamp))
}
