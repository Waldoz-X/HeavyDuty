package com.example.heavyduty.feature.stats.presentation.informemensual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.extensions.formatCompact
import com.example.heavyduty.core.ui.components.HdBarChart
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.nameResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformeMensualScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InformeMensualViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informe mensual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdLoadingIndicator(modifier = Modifier.padding(innerPadding), message = "Cargando...")
            return@Scaffold
        }

        val report = uiState.report
        
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = report.monthLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportStatCard(
                        icon = Icons.Default.CalendarMonth,
                        label = "Entrenamientos", 
                        value = "${report.totalWorkouts}", 
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatCard(
                        icon = Icons.Default.TrendingUp,
                        label = "Series", 
                        value = "${report.totalSets}", 
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReportStatCard(
                        icon = Icons.Default.FitnessCenter,
                        label = "Volumen total",
                        value = "${report.totalVolumeKg.formatCompact()} kg",
                        modifier = Modifier.weight(1f),
                    )
                    ReportStatCard(
                        icon = Icons.Default.Timer,
                        label = "Duración prom.",
                        value = "${report.avgSessionDurationMinutes}min",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            
            val topMuscleGroup = report.topMuscleGroup
            if (topMuscleGroup != null) {
                item {
                    ReportStatCard(
                        icon = Icons.Default.FitnessCenter,
                        label = "Músculo más trabajado",
                        value = stringResource(topMuscleGroup.nameResId),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (report.workoutsPerWeek.isNotEmpty()) {
                item {
                    HdCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Entrenamientos por Semana",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HdBarChart(
                            data = report.workoutsPerWeek.map { it.first to it.second.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }
            }

            if (report.volumePerWeek.isNotEmpty()) {
                item {
                    HdCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Volumen por Semana (kg)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HdBarChart(
                            data = report.volumePerWeek,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.Start) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
