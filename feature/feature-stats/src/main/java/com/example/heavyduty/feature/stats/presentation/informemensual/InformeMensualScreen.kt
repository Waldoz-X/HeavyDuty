package com.example.heavyduty.feature.stats.presentation.informemensual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.core.common.extensions.formatCompact
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdLoadingIndicator

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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Text(
                text = report.monthLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportStatCard(label = "Entrenamientos", value = "${report.totalWorkouts}", modifier = Modifier.weight(1f))
                ReportStatCard(label = "Series", value = "${report.totalSets}", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportStatCard(
                    label = "Volumen total",
                    value = "${report.totalVolumeKg.formatCompact()} kg",
                    modifier = Modifier.weight(1f),
                )
                ReportStatCard(
                    label = "Duración promedio",
                    value = "${report.avgSessionDurationMinutes}min",
                    modifier = Modifier.weight(1f),
                )
            }
            val topMuscleGroup = report.topMuscleGroup
            if (topMuscleGroup != null) {
                Spacer(modifier = Modifier.height(12.dp))
                ReportStatCard(
                    label = "Músculo más trabajado",
                    value = stringResource(topMuscleGroup.nameResId),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReportStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
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
