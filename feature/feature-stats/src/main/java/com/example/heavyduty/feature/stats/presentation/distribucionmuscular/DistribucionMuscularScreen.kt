package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdPeriodSelector
import com.example.heavyduty.core.ui.components.HdRadarChart
import com.example.heavyduty.core.ui.components.HdStatCard
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistribucionMuscularScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DistribucionMuscularViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_radar_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when {
            uiState.isLoading -> HdCardsSkeleton(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                heights = listOf(48.dp, 320.dp, 120.dp, 120.dp)
            )

            uiState.loadFailed -> HdErrorState(modifier = Modifier.padding(innerPadding))

            uiState.currentWeek.size < 3 -> HdEmptyState(
                icon = Icons.Default.PieChart,
                title = stringResource(R.string.stats_insufficient_data),
                description = stringResource(R.string.stats_radar_need_three),
                modifier = Modifier.padding(innerPadding)
            )

            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HdPeriodSelector(
                    selected = uiState.selectedPeriod,
                    onSelect = { viewModel.onEvent(DistribucionMuscularUiEvent.OnPeriodChange(it)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.stats_current))
                    LegendDot(color = MaterialTheme.colorScheme.secondary, label = stringResource(R.string.stats_previous))
                }
                HdRadarChart(
                    data = uiState.currentWeek.mapKeys { (muscle, _) -> stringResource(muscle.nameResId) },
                    secondaryData = uiState.previousWeek.mapKeys { (muscle, _) -> stringResource(muscle.nameResId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                PeriodTotals(summary = uiState.summary)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** Workouts / duration / volume / sets tiles, each with a delta vs. the previous period. */
@Composable
private fun PeriodTotals(summary: PeriodSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HdStatCard(
                icon = Icons.Default.CalendarMonth,
                label = stringResource(R.string.stats_workouts),
                value = "${summary.workouts}",
                deltaText = vsPrevious(signed(summary.workoutsDelta)),
                isPositive = summary.workoutsDelta >= 0,
                modifier = Modifier.weight(1f)
            )
            HdStatCard(
                icon = Icons.Default.Timer,
                label = stringResource(R.string.metric_duration),
                value = durationText(summary.durationMinutes),
                deltaText = vsPrevious(signOf(summary.durationMinutesDelta >= 0) + durationText(abs(summary.durationMinutesDelta))),
                isPositive = summary.durationMinutesDelta >= 0,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HdStatCard(
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.metric_volume),
                value = summary.volumeKg.formatTotalWeight(),
                deltaText = vsPrevious(signOf(summary.volumeKgDelta >= 0) + abs(summary.volumeKgDelta).formatTotalWeight()),
                isPositive = summary.volumeKgDelta >= 0,
                modifier = Modifier.weight(1f)
            )
            HdStatCard(
                icon = Icons.Default.Repeat,
                label = stringResource(R.string.stats_sets),
                value = "${summary.sets}",
                deltaText = vsPrevious(signed(summary.setsDelta)),
                isPositive = summary.setsDelta >= 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** "1 h 5 min", or "45 min" under an hour. */
@Composable
private fun durationText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return if (hours > 0) stringResource(R.string.duration_hours_minutes, hours, rest) else stringResource(R.string.duration_minutes, rest)
}

/** "+2 vs. anterior". */
@Composable
private fun vsPrevious(change: String): String = stringResource(R.string.stats_vs_previous, change)

/** "+2", "0" reads "+0", "-3" keeps its own sign. */
private fun signed(delta: Int): String = if (delta >= 0) "+$delta" else "$delta"

private fun signOf(isUpOrSame: Boolean): String = if (isUpOrSame) "+" else "-"

private const val MINUTES_PER_HOUR = 60
