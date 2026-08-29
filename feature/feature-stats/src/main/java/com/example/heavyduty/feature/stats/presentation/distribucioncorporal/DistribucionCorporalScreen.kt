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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.components.HdBodyDiagram
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistribucionCorporalScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DistribucionCorporalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Distribución corporal") },
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

        val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val fatiguedColor = MaterialTheme.colorScheme.error

        val categoryColors: Map<String, Color>? = if (uiState.mode == BodyMapMode.FATIGUE) {
            uiState.fatigueByMuscle.mapValues { (_, level) -> lerp(baseColor, fatiguedColor, level.coerceIn(0f, 1f)) }
        } else {
            null
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HdChip(
                    text = "Volumen",
                    isSelected = uiState.mode == BodyMapMode.VOLUME,
                    onClick = { viewModel.onEvent(DistribucionCorporalUiEvent.OnModeChange(BodyMapMode.VOLUME)) },
                )
                HdChip(
                    text = "Fatiga",
                    isSelected = uiState.mode == BodyMapMode.FATIGUE,
                    onClick = { viewModel.onEvent(DistribucionCorporalUiEvent.OnModeChange(BodyMapMode.FATIGUE)) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.mode == BodyMapMode.FATIGUE) {
                FatigueGradientLegend(baseColor = baseColor, fatiguedColor = fatiguedColor)
                Spacer(modifier = Modifier.height(12.dp))
            }

            HdBodyDiagram(
                muscleValues = uiState.volumeByMuscle,
                gender = uiState.gender,
                baseColor = baseColor,
                categoryColors = categoryColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
            )
        }
    }
}

@Composable
private fun FatigueGradientLegend(baseColor: Color, fatiguedColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Brush.horizontalGradient(listOf(baseColor, fatiguedColor))),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Fresco",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Fatigado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
