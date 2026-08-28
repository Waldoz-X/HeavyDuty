package com.example.heavyduty.feature.stats.presentation.main

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.components.HdBodyDiagram
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdLoadingIndicator

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
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
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
            HdLoadingIndicator(modifier = Modifier.padding(innerPadding), message = "Cargando estadísticas...")
            return@Scaffold
        }

        val advancedStats = listOf(
            AdvancedStatRow(
                icon = Icons.Default.Timeline,
                title = "Recuento de series por grupo de músculos",
                description = "Número de series registradas para cada grupo de músculos.",
                onClick = onNavigateToSeries,
            ),
            AdvancedStatRow(
                icon = Icons.Default.PieChart,
                title = "Distribución de los músculos (gráfico)",
                description = "Compara las distribuciones actual y previa de los músculos.",
                onClick = onNavigateToDistribucionRadar,
            ),
            AdvancedStatRow(
                icon = Icons.Default.Accessibility,
                title = "Distribución de los músculos (cuerpo)",
                description = "Mapa semanal de volumen o fatiga por músculo.",
                onClick = onNavigateToDistribucionCuerpo,
            ),
            AdvancedStatRow(
                icon = Icons.Default.EmojiEvents,
                title = "Ejercicios principales",
                description = "Lista de ejercicios que realizas con más frecuencia.",
                onClick = onNavigateToPrincipales,
            ),
            AdvancedStatRow(
                icon = Icons.Default.Leaderboard,
                title = "Ejercicios de clasificación",
                description = "Lista de ejercicios elegibles para la clasificación.",
                onClick = onNavigateToClasificacion,
            ),
            AdvancedStatRow(
                icon = Icons.Default.CalendarMonth,
                title = "Informe mensual",
                description = "Resumen de tus entrenamientos y estadísticas mensuales.",
                onClick = onNavigateToInformeMensual,
            ),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Gráfico corporal de los últimos siete días",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    uiState.days.forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HdChip(
                                text = day.dayLetter,
                                isSelected = day.isSelected,
                                onClick = { viewModel.onEvent(EstadisticasUiEvent.OnDaySelected(day.dateMillis)) },
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = day.dayNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                HdBodyDiagram(
                    muscleValues = uiState.selectedDayVolumeByMuscle,
                    gender = uiState.gender,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )
            }

            item {
                Text(
                    text = "Estadísticas avanzadas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            items(advancedStats) { stat ->
                AdvancedStatCard(stat)
            }
        }
    }
}

private data class AdvancedStatRow(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
)

@Composable
private fun AdvancedStatCard(stat: AdvancedStatRow, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.clickable(onClick = stat.onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stat.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
