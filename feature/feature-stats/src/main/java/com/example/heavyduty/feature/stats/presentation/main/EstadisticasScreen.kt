package com.example.heavyduty.feature.stats.presentation.main

import androidx.annotation.StringRes
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.components.BodyGender
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
    val open: (AdvancedStat) -> Unit = { stat ->
        when (stat) {
            AdvancedStat.SERIES -> onNavigateToSeries()
            AdvancedStat.RADAR -> onNavigateToDistribucionRadar()
            AdvancedStat.BODY -> onNavigateToDistribucionCuerpo()
            AdvancedStat.TOP -> onNavigateToPrincipales()
            AdvancedStat.RECORDS -> onNavigateToClasificacion()
            AdvancedStat.MONTHLY -> onNavigateToInformeMensual()
            AdvancedStat.ACTIVITY -> onNavigateToMapaActividad()
            AdvancedStat.BODY_WEIGHT -> onNavigateToPesoCorporal()
        }
    }

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
        when {
            uiState.isLoading -> HdCardsSkeleton(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                heights = listOf(140.dp, 220.dp, 120.dp, 120.dp)
            )

            uiState.loadFailed -> HdErrorState(modifier = Modifier.padding(innerPadding))

            else -> StatsContent(
                uiState = uiState,
                onDaySelect = { viewModel.onEvent(EstadisticasUiEvent.OnDaySelected(it)) },
                onOpen = open,
                // The top bar's height: without it the first cards sat hidden under the title.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun StatsContent(
    uiState: EstadisticasUiState,
    onDaySelect: (Long) -> Unit,
    onOpen: (AdvancedStat) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        item { SummaryCard(summary = uiState.summary) }
        item {
            Text(
                text = stringResource(R.string.stats_muscles_by_day),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        item { DayPicker(days = uiState.days, onSelect = onDaySelect) }
        item { DayBodyDiagram(volumeByMuscle = uiState.selectedDayVolumeByMuscle, gender = uiState.gender) }
        item {
            Text(
                text = stringResource(R.string.stats_advanced),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(AdvancedStat.entries) { stat -> AdvancedStatCard(stat = stat, onClick = { onOpen(stat) }) }
    }
}

/** The last seven days; the selected one's muscles show on the diagram below. */
@Composable
private fun DayPicker(days: List<DayChipUi>, onSelect: (Long) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HdChip(text = day.dayLetter, isSelected = day.isSelected, onClick = { onSelect(day.dateMillis) })
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = day.dayNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayBodyDiagram(volumeByMuscle: Map<MuscleGroup, Float>, gender: BodyGender, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HdBodyDiagram(muscleValues = volumeByMuscle, gender = gender, modifier = Modifier.fillMaxWidth().height(240.dp))
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
}

/** The deeper statistics, each a screen of its own. */
private enum class AdvancedStat(val icon: ImageVector, @param:StringRes val title: Int, @param:StringRes val description: Int) {
    SERIES(Icons.Default.Timeline, R.string.stats_series_title, R.string.stats_series_desc),
    RADAR(Icons.Default.PieChart, R.string.stats_radar_title, R.string.stats_radar_desc),
    BODY(Icons.Default.Accessibility, R.string.stats_body_title, R.string.stats_body_desc),
    TOP(Icons.Default.EmojiEvents, R.string.stats_top_title, R.string.stats_top_desc),
    RECORDS(Icons.Default.Leaderboard, R.string.stats_records_title, R.string.stats_records_desc),
    MONTHLY(Icons.Default.CalendarMonth, R.string.stats_monthly_title, R.string.stats_monthly_desc),
    ACTIVITY(Icons.Default.DateRange, R.string.stats_activity_title, R.string.stats_activity_desc),
    BODY_WEIGHT(Icons.Default.Timeline, R.string.stats_bodyweight_title, R.string.stats_bodyweight_desc)
}

@Composable
private fun AdvancedStatCard(stat: AdvancedStat, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier, onClick = onClick) {
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
                    text = stringResource(stat.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(stat.description),
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
