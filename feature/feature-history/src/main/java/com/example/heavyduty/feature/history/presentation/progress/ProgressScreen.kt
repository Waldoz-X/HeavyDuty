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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
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
fun ProgressScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProgressUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progreso del Ejercicio",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(ProgressUiEvent.OnNavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                HdLoadingIndicator(
                    modifier = Modifier.padding(innerPadding),
                    message = "Cargando progreso...",
                )
            }
            uiState.sets.isEmpty() -> {
                HdEmptyState(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    title = "Aún no hay datos",
                    description = "Completa series de este ejercicio para ver tu progreso",
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
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
                        HdCard {
                            Text(
                                text = "Peso a lo Largo del Tiempo",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            WeightProgressChart(
                                weights = chronologicalWeights,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Weight progress summary
                    HdCard {
                        Text(
                            text = "Progreso de Peso",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val weightSets = uiState.sets.filter { (it.weight ?: 0f) > 0f }
                        if (weightSets.isNotEmpty()) {
                            val maxWeight = weightSets.maxOf { it.weight ?: 0f }.kgTo(uiState.weightUnit)
                            val latestWeight = (weightSets.first().weight ?: 0f).kgTo(uiState.weightUnit)
                            Text(
                                text = "Máx: ${maxWeight.formatDecimal()}$unitLabel · Último: ${latestWeight.formatDecimal()}$unitLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${weightSets.size} series registradas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "No hay datos de peso disponibles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Volume progress summary
                    HdCard {
                        Text(
                            text = "Progreso de Volumen",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val repSets = uiState.sets.filter { (it.reps ?: 0) > 0 }
                        if (repSets.isNotEmpty()) {
                            val totalReps = repSets.sumOf { it.reps ?: 0 }
                            val totalVolume = repSets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }
                                .toFloat()
                                .kgTo(uiState.weightUnit)
                            Text(
                                text = "Repeticiones Totales: $totalReps · Volumen: ${totalVolume.formatDecimal()}$unitLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            Text(
                                text = "No hay datos de repeticiones disponibles",
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

@Composable
private fun PersonalRecordsCard(
    records: ExercisePersonalRecords,
    modifier: Modifier = Modifier,
    weightUnit: WeightUnit = WeightUnit.KG,
) {
    val unitLabel = if (weightUnit == WeightUnit.LB) "lb" else "kg"
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Récords Personales",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PersonalRecordItem(
                label = "Peso Máximo",
                value = records.maxWeight?.let { "${it.kgTo(weightUnit).formatDecimal()}$unitLabel" } ?: "—",
                detail = records.maxWeightReps?.let { "× $it reps" },
            )
            PersonalRecordItem(
                label = "Más Repeticiones",
                value = records.maxReps?.toString() ?: "—",
                detail = records.maxRepsWeight?.let { "@ ${it.kgTo(weightUnit).formatDecimal()}$unitLabel" },
            )
            PersonalRecordItem(
                label = "1RM Estimado",
                value = records.estimatedOneRepMax?.let { "${it.kgTo(weightUnit).formatDecimal()}$unitLabel" } ?: "—",
                detail = null,
            )
        }
    }
}

@Composable
private fun PersonalRecordItem(
    label: String,
    value: String,
    detail: String?,
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeightProgressChart(
    weights: List<Float>,
    modifier: Modifier = Modifier,
) {
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
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
            modelProducer = modelProducer,
            modifier = modifier,
        )
    }
}
