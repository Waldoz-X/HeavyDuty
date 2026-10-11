package com.example.heavyduty.feature.history.presentation.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.feature.history.domain.model.ExercisePersonalRecords
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ProgressViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProgressUiEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.progress_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(ProgressUiEvent.OnNavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                HdCardsSkeleton(modifier = Modifier.padding(innerPadding).padding(16.dp), heights = listOf(240.dp, 240.dp, 140.dp))
            }

            uiState.loadFailed -> HdErrorState(modifier = Modifier.padding(innerPadding))

            uiState.sets.isEmpty() -> {
                HdEmptyState(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    title = stringResource(R.string.progress_empty_title),
                    description = stringResource(R.string.progress_empty_description),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> ProgressContent(uiState = uiState, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ProgressContent(uiState: ProgressUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val unitLabel = if (uiState.weightUnit == WeightUnit.LB) "lb" else "kg"

        if (uiState.personalRecords.hasRecords) {
            PersonalRecordsCard(records = uiState.personalRecords, weightUnit = uiState.weightUnit)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val chronologicalWeights = uiState.sets
            .filter { (it.weight ?: 0f) > 0f }
            .sortedBy { it.completedAt ?: 0L }
            .map { it.weight!!.kgTo(uiState.weightUnit) }

        if (chronologicalWeights.size >= 2) {
            WeightOverTimeCard(weights = chronologicalWeights)
            Spacer(modifier = Modifier.height(16.dp))
        }

        WeightSummaryCard(sets = uiState.sets, weightUnit = uiState.weightUnit, unitLabel = unitLabel)

        Spacer(modifier = Modifier.height(16.dp))

        VolumeSummaryCard(sets = uiState.sets, weightUnit = uiState.weightUnit, unitLabel = unitLabel)
    }
}

/** The working weight set by set, oldest first. */
@Composable
private fun WeightOverTimeCard(weights: List<Float>) {
    HdCard {
        Text(
            text = stringResource(R.string.progress_weight_over_time),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        WeightProgressChart(
            weights = weights,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
    }
}

/** Heaviest and latest weight, and how many sets carried one. */
@Composable
private fun WeightSummaryCard(sets: List<WorkoutSet>, weightUnit: WeightUnit, unitLabel: String) {
    HdCard {
        Text(
            text = stringResource(R.string.progress_weight),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        val weightSets = sets.filter { (it.weight ?: 0f) > 0f }
        if (weightSets.isNotEmpty()) {
            val maxWeight = weightSets.maxOf { it.weight ?: 0f }.kgTo(weightUnit)
            val latestWeight = (weightSets.first().weight ?: 0f).kgTo(weightUnit)
            Text(
                text = stringResource(
                    R.string.progress_max_latest,
                    "${maxWeight.formatDecimal()} $unitLabel",
                    "${latestWeight.formatDecimal()} $unitLabel"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = pluralStringResource(R.plurals.progress_sets_logged, weightSets.size, weightSets.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.progress_no_weight_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Total reps and volume across every logged set. */
@Composable
private fun VolumeSummaryCard(sets: List<WorkoutSet>, weightUnit: WeightUnit, unitLabel: String) {
    HdCard {
        Text(
            text = stringResource(R.string.progress_volume),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        val repSets = sets.filter { (it.reps ?: 0) > 0 }
        if (repSets.isNotEmpty()) {
            val totalReps = repSets.sumOf { it.reps ?: 0 }
            val totalVolume = repSets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }
                .toFloat()
                .kgTo(weightUnit)
            Text(
                text = stringResource(
                    R.string.progress_reps_volume,
                    totalReps,
                    "${totalVolume.formatDecimal()} $unitLabel"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = stringResource(R.string.progress_no_reps_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PersonalRecordsCard(records: ExercisePersonalRecords, modifier: Modifier = Modifier, weightUnit: WeightUnit = WeightUnit.KG) {
    val unitLabel = if (weightUnit == WeightUnit.LB) "lb" else "kg"
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.progress_records),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PersonalRecordItem(
                label = stringResource(R.string.progress_max_weight),
                value = records.maxWeight?.let { "${it.kgTo(weightUnit).formatDecimal()} $unitLabel" } ?: "—",
                detail = records.maxWeightReps?.let { pluralStringResource(R.plurals.progress_times_reps, it, it) }
            )
            PersonalRecordItem(
                label = stringResource(R.string.progress_most_reps),
                value = records.maxReps?.toString() ?: "—",
                detail = records.maxRepsWeight?.let { "@ ${it.kgTo(weightUnit).formatDecimal()}$unitLabel" }
            )
            PersonalRecordItem(
                label = stringResource(R.string.progress_estimated_1rm),
                value = records.estimatedOneRepMax?.let { "${it.kgTo(weightUnit).formatDecimal()}$unitLabel" } ?: "—",
                detail = null
            )
        }
    }
}

@Composable
private fun PersonalRecordItem(label: String, value: String, detail: String?) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightProgressChart(weights: List<Float>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(weights) {
        modelProducer.runTransaction {
            lineModel { series(y = weights) }
        }
    }

    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom()
            ),
            modelProducer = modelProducer,
            modifier = modifier
        )
    }
}
