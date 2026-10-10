package com.example.heavyduty.feature.stats.presentation.main

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.ui.components.HdBodyDiagram
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToDistribucionRadar: () -> Unit,
    onNavigateToDistribucionCuerpo: () -> Unit,
    onNavigateToPrincipales: () -> Unit,
    onNavigateToClasificacion: () -> Unit,
    onNavigateToInformeMensual: () -> Unit,
    onNavigateToMapaActividad: () -> Unit,
    onNavigateToPesoCorporal: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdCardsSkeleton(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                heights = listOf(140.dp, 220.dp, 120.dp, 120.dp)
            )
            return@Scaffold
        }
        if (uiState.loadFailed) {
            HdErrorState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        val advancedStats = listOf(
            AdvancedStatRow(
                icon = Icons.Default.Timeline,
                title = stringResource(R.string.stats_series_title),
                description = stringResource(R.string.stats_series_desc),
                onClick = onNavigateToSeries
            ),
            AdvancedStatRow(
                icon = Icons.Default.PieChart,
                title = stringResource(R.string.stats_radar_title),
                description = stringResource(R.string.stats_radar_desc),
                onClick = onNavigateToDistribucionRadar
            ),
            AdvancedStatRow(
                icon = Icons.Default.Accessibility,
                title = stringResource(R.string.stats_body_title),
                description = stringResource(R.string.stats_body_desc),
                onClick = onNavigateToDistribucionCuerpo
            ),
            AdvancedStatRow(
                icon = Icons.Default.EmojiEvents,
                title = stringResource(R.string.stats_top_title),
                description = stringResource(R.string.stats_top_desc),
                onClick = onNavigateToPrincipales
            ),
            AdvancedStatRow(
                icon = Icons.Default.Leaderboard,
                title = stringResource(R.string.stats_records_title),
                description = stringResource(R.string.stats_records_desc),
                onClick = onNavigateToClasificacion
            ),
            AdvancedStatRow(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.stats_monthly_title),
                description = stringResource(R.string.stats_monthly_desc),
                onClick = onNavigateToInformeMensual
            ),
            AdvancedStatRow(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.stats_activity_title),
                description = stringResource(R.string.stats_activity_desc),
                onClick = onNavigateToMapaActividad
            ),
            AdvancedStatRow(
                icon = Icons.Default.Timeline,
                title = stringResource(R.string.stats_bodyweight_title),
                description = stringResource(R.string.stats_bodyweight_desc),
                onClick = onNavigateToPesoCorporal
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            // The top bar's height: without it the first cards sat hidden under the title.
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SummaryCard(summary = uiState.summary)
            }

            item {
                Text(
                    text = stringResource(R.string.stats_muscles_by_day),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    uiState.days.forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HdChip(
                                text = day.dayLetter,
                                isSelected = day.isSelected,
                                onClick = { viewModel.onEvent(EstadisticasUiEvent.OnDaySelected(day.dateMillis)) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = day.dayNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                val volumeByMuscle = uiState.selectedDayVolumeByMuscle
                HdBodyDiagram(
                    muscleValues = volumeByMuscle,
                    gender = uiState.gender,
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
                if (volumeByMuscle.isEmpty()) {
                    Text(
                        text = stringResource(R.string.stats_no_workout_day),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.stats_advanced),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(advancedStats) { stat ->
                AdvancedStatCard(stat)
            }
        }
    }
}

private data class AdvancedStatRow(val icon: ImageVector, val title: String, val description: String, val onClick: () -> Unit)

@Composable
private fun AdvancedStatCard(stat: AdvancedStatRow, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier, onClick = stat.onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stat.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Last 30 days at a glance, each figure against the 30 days before. */
@Composable
private fun SummaryCard(summary: PeriodSummary) {
    HdCard {
        Text(
            text = stringResource(R.string.period_last_30_days),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryFigure(
                stringResource(R.string.stats_workouts),
                summary.workouts.toString(),
                signed(summary.workoutsDelta),
                summary.workoutsDelta >= 0,
                Modifier.weight(1f)
            )
            SummaryFigure(
                stringResource(R.string.stats_sets),
                summary.sets.toString(),
                signed(summary.setsDelta),
                summary.setsDelta >= 0,
                Modifier.weight(1f)
            )
            val volumeDelta = summary.volumeKgDelta
            SummaryFigure(
                label = stringResource(R.string.metric_volume),
                value = summary.volumeKg.formatTotalWeight(),
                delta = (if (volumeDelta >= 0) "+" else "−") + kotlin.math.abs(volumeDelta).formatTotalWeight(),
                isPositive = volumeDelta >= 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, delta: String, isPositive: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = delta,
            style = MaterialTheme.typography.labelSmall,
            color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else "−${-value}"
