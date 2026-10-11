package com.example.heavyduty.feature.stats.presentation.distribucioncorporal

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.components.HdBodyDiagram
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdPeriodSelector
import com.example.heavyduty.core.ui.components.HdStatCard
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.feature.stats.domain.model.PeriodSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistribucionCorporalScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DistribucionCorporalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_body_screen_title)) },
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
                heights = listOf(48.dp, 400.dp, 120.dp)
            )

            uiState.loadFailed -> HdErrorState(modifier = Modifier.padding(innerPadding))

            else -> BodyMapContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun BodyMapContent(
    uiState: DistribucionCorporalUiState,
    onEvent: (DistribucionCorporalUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fatiguedColor = MaterialTheme.colorScheme.error
    val strongColor = MaterialTheme.colorScheme.tertiary
    Column(modifier = modifier) {
        HdPeriodSelector(selected = uiState.selectedPeriod, onSelect = { onEvent(DistribucionCorporalUiEvent.OnPeriodChange(it)) })
        Spacer(modifier = Modifier.height(12.dp))
        ModeChips(mode = uiState.mode, onSelect = { onEvent(DistribucionCorporalUiEvent.OnModeChange(it)) })
        Spacer(modifier = Modifier.height(12.dp))
        when (uiState.mode) {
            BodyMapMode.FATIGUE -> {
                GradientLegend(
                    startColor = baseColor,
                    endColor = fatiguedColor,
                    startLabel = stringResource(R.string.stats_fresh),
                    endLabel = stringResource(R.string.stats_fatigued)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            BodyMapMode.STRENGTH -> {
                GradientLegend(
                    startColor = baseColor,
                    endColor = strongColor,
                    startLabel = stringResource(R.string.stats_long_ago),
                    endLabel = stringResource(R.string.stats_recently_trained)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            BodyMapMode.VOLUME -> Unit
        }
        HdBodyDiagram(
            muscleValues = uiState.volumeByMuscle,
            gender = uiState.gender,
            baseColor = baseColor,
            categoryColors = muscleColors(uiState, baseColor = baseColor, fatiguedColor = fatiguedColor, strongColor = strongColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        BodyPeriodTotals(summary = uiState.summary)
    }
}

@Composable
private fun ModeChips(mode: BodyMapMode, onSelect: (BodyMapMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            BodyMapMode.VOLUME to R.string.metric_volume,
            BodyMapMode.FATIGUE to R.string.stats_fatigue,
            BodyMapMode.STRENGTH to R.string.metric_strength
        ).forEach { (option, label) ->
            HdChip(text = stringResource(label), isSelected = mode == option, onClick = { onSelect(option) })
        }
    }
}

/**
 * Each muscle's colour in the fatigue and strength modes, from [baseColor] towards the mode's
 * colour; null in volume mode, where the diagram shades by volume itself.
 */
private fun muscleColors(
    uiState: DistribucionCorporalUiState,
    baseColor: Color,
    fatiguedColor: Color,
    strongColor: Color
): Map<MuscleGroup, Color>? = when (uiState.mode) {
    BodyMapMode.FATIGUE -> uiState.fatigueByMuscle.entries.associate { (muscle, level) ->
        muscle to lerp(baseColor, fatiguedColor, level.coerceIn(0f, 1f))
    }

    BodyMapMode.STRENGTH -> uiState.strengthByMuscle.entries.associate { (muscle, retention) ->
        // Retention floors at 0.5 (see strengthRetention), so normalize that range to
        // 0..1 before interpolating — otherwise the floor would never read as "base."
        val fraction = ((retention - 0.5f) / 0.5f).coerceIn(0f, 1f)
        muscle to lerp(baseColor, strongColor, fraction)
    }

    BodyMapMode.VOLUME -> null
}

/** Workouts / duration / volume / sets tiles, each with a delta vs. the previous period. */
@Composable
private fun BodyPeriodTotals(summary: PeriodSummary) {
    val resources = LocalResources.current
    fun deltaText(delta: Int): String {
        val sign = if (delta >= 0) "+" else ""
        return resources.getString(R.string.stats_vs_previous, "$sign$delta")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HdStatCard(
                icon = Icons.Default.CalendarMonth,
                label = stringResource(R.string.stats_workouts),
                value = "${summary.workouts}",
                deltaText = deltaText(summary.workoutsDelta),
                isPositive = summary.workoutsDelta >= 0,
                modifier = Modifier.weight(1f)
            )
            HdStatCard(
                icon = Icons.Default.Timer,
                label = stringResource(R.string.metric_duration),
                value = run {
                    val h = summary.durationMinutes / 60
                    val m = summary.durationMinutes % 60
                    if (h > 0) stringResource(R.string.duration_hours_minutes, h, m) else stringResource(R.string.duration_minutes, m)
                },
                subtext = null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HdStatCard(
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.metric_volume),
                value = summary.volumeKg.formatTotalWeight(),
                deltaText = run {
                    val sign = if (summary.volumeKgDelta >= 0) "+" else "-"
                    stringResource(R.string.stats_vs_previous, "$sign${kotlin.math.abs(summary.volumeKgDelta).formatTotalWeight()}")
                },
                isPositive = summary.volumeKgDelta >= 0,
                modifier = Modifier.weight(1f)
            )
            HdStatCard(
                icon = Icons.Default.Repeat,
                label = stringResource(R.string.stats_sets),
                value = "${summary.sets}",
                deltaText = deltaText(summary.setsDelta),
                isPositive = summary.setsDelta >= 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GradientLegend(startColor: Color, endColor: Color, startLabel: String, endLabel: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Brush.horizontalGradient(listOf(startColor, endColor)))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
