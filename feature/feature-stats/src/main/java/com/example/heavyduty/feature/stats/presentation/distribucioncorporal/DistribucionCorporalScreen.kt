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
        val strongColor = MaterialTheme.colorScheme.tertiary

        val categoryColors: Map<String, Color>? = when (uiState.mode) {
            BodyMapMode.FATIGUE -> uiState.fatigueByMuscle.mapValues { (_, level) ->
                lerp(baseColor, fatiguedColor, level.coerceIn(0f, 1f))
            }
            BodyMapMode.STRENGTH -> uiState.strengthByMuscle.mapValues { (_, retention) ->
                // Retention floors at 0.5 (see strengthRetention), so normalize that range to
                // 0..1 before interpolating — otherwise the floor would never read as "base."
                val fraction = ((retention - 0.5f) / 0.5f).coerceIn(0f, 1f)
                lerp(baseColor, strongColor, fraction)
            }
            BodyMapMode.VOLUME -> null
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
                HdChip(
                    text = "Fuerza",
                    isSelected = uiState.mode == BodyMapMode.STRENGTH,
                    onClick = { viewModel.onEvent(DistribucionCorporalUiEvent.OnModeChange(BodyMapMode.STRENGTH)) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.mode) {
                BodyMapMode.FATIGUE -> {
                    GradientLegend(startColor = baseColor, endColor = fatiguedColor, startLabel = "Fresco", endLabel = "Fatigado")
                    Spacer(modifier = Modifier.height(12.dp))
                }
                BodyMapMode.STRENGTH -> {
                    GradientLegend(startColor = baseColor, endColor = strongColor, startLabel = "Hace tiempo", endLabel = "Recién entrenado")
                    Spacer(modifier = Modifier.height(12.dp))
                }
                BodyMapMode.VOLUME -> Unit
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
private fun GradientLegend(
    startColor: Color,
    endColor: Color,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Brush.horizontalGradient(listOf(startColor, endColor))),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
