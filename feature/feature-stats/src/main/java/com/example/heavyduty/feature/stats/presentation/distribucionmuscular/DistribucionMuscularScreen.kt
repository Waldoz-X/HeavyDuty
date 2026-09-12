package com.example.heavyduty.feature.stats.presentation.distribucionmuscular

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.components.HdRadarChart
import com.example.heavyduty.core.ui.nameResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistribucionMuscularScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DistribucionMuscularViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Distribución muscular") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        when {
            uiState.isLoading -> HdLoadingIndicator(modifier = Modifier.padding(innerPadding), message = "Cargando...")
            uiState.currentWeek.size < 3 -> HdEmptyState(
                icon = Icons.Default.PieChart,
                title = "Datos insuficientes",
                description = "Entrena al menos 3 grupos musculares distintos para ver la comparación.",
                modifier = Modifier.padding(innerPadding),
            )
            else -> Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(color = MaterialTheme.colorScheme.primary, label = "Esta semana")
                    LegendDot(color = MaterialTheme.colorScheme.secondary, label = "Semana anterior")
                }
                HdRadarChart(
                    data = uiState.currentWeek.mapKeys { (muscle, _) -> stringResource(muscle.nameResId) },
                    secondaryData = uiState.previousWeek.mapKeys { (muscle, _) -> stringResource(muscle.nameResId) },
                    modifier = Modifier.fillMaxSize(),
                )
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
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
