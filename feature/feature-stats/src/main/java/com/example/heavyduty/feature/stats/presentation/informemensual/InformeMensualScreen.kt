package com.example.heavyduty.feature.stats.presentation.informemensual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.extensions.formatCompact
import com.example.heavyduty.core.ui.components.HdBarChart
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.feature.stats.domain.model.MuscleVolumeContribution
import com.example.heavyduty.feature.stats.domain.model.TopExerciseMonthly

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
                title = { Text("Informe Mensual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Cabecera interactiva con selector de meses (< Mes Año >)
            MonthSelectorHeader(
                monthLabel = uiState.report.monthLabel,
                hasNextMonth = uiState.hasNextMonth,
                onPreviousMonth = { viewModel.selectPreviousMonth() },
                onNextMonth = { viewModel.selectNextMonth() },
            )

            if (uiState.isLoading) {
                HdLoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    message = "Calculando métricas del mes...",
                )
                return@Scaffold
            }

            val report = uiState.report

            if (report.totalWorkouts == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HdEmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = "Sin entrenamientos",
                        description = "No se registraron entrenamientos completados en ${report.monthLabel.lowercase()}.",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Tarjetas de métricas principales con badges comparativos
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                EnrichedStatCard(
                                    icon = Icons.Default.FitnessCenter,
                                    label = "Volumen Total",
                                    value = "${report.totalVolumeKg.formatCompact()} kg",
                                    deltaText = report.prevMonthVolumeDeltaPercent?.let {
                                        val sign = if (it >= 0) "+" else ""
                                        "$sign${"%.1f".format(it)}% vs mes ant."
                                    },
                                    isPositive = (report.prevMonthVolumeDeltaPercent ?: 0f) >= 0,
                                    modifier = Modifier.weight(1f),
                                )
                                EnrichedStatCard(
                                    icon = Icons.Default.CalendarMonth,
                                    label = "Entrenamientos",
                                    value = "${report.totalWorkouts}",
                                    deltaText = report.prevMonthWorkoutsDelta?.let {
                                        val sign = if (it >= 0) "+" else ""
                                        "$sign$it vs mes ant."
                                    },
                                    isPositive = (report.prevMonthWorkoutsDelta ?: 0) >= 0,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val hours = report.totalDurationMinutes / 60
                                val mins = report.totalDurationMinutes % 60
                                val durationStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                                EnrichedStatCard(
                                    icon = Icons.Default.Timer,
                                    label = "Tiempo Total",
                                    value = durationStr,
                                    subtext = "Promedio: ${report.avgSessionDurationMinutes} min/sesión",
                                    modifier = Modifier.weight(1f),
                                )
                                EnrichedStatCard(
                                    icon = Icons.Default.Repeat,
                                    label = "Series Efectivas",
                                    value = "${report.totalSets}",
                                    subtext = "${"%.1f".format(report.workoutsPerWeekAverage)} sesiones / sem",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // 2. Desglose de distribución muscular
                    if (report.muscleContributions.isNotEmpty()) {
                        item {
                            MuscleDistributionCard(
                                contributions = report.muscleContributions,
                                topMuscle = report.topMuscleGroup,
                            )
                        }
                    }

                    // 3. Top ejercicios del mes
                    if (report.topExercises.isNotEmpty()) {
                        item {
                            TopExercisesCard(exercises = report.topExercises)
                        }
                    }

                    // 4. Gráfica de Entrenamientos por semana
                    if (report.workoutsPerWeek.isNotEmpty()) {
                        item {
                            HdCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Entrenamientos por Semana",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                HdBarChart(
                                    data = report.workoutsPerWeek.map { it.first to it.second.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                )
                            }
                        }
                    }

                    // 5. Gráfica de Volumen por semana
                    if (report.volumePerWeek.isNotEmpty()) {
                        item {
                            HdCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Volumen por Semana (kg)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                HdBarChart(
                                    data = report.volumePerWeek,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorHeader(
    monthLabel: String,
    hasNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Mes anterior",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        IconButton(
            onClick = onNextMonth,
            enabled = hasNextMonth,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Mes siguiente",
                tint = if (hasNextMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun EnrichedStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    deltaText: String? = null,
    isPositive: Boolean = true,
    subtext: String? = null,
) {
    HdCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            if (deltaText != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPositive) Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else Color(0xFFE53935).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE53935),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (subtext != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun MuscleDistributionCard(
    contributions: List<MuscleVolumeContribution>,
    topMuscle: com.example.heavyduty.core.domain.model.MuscleGroup?,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Distribución Muscular",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (topMuscle != null) {
                Text(
                    text = "Top: ${stringResource(topMuscle.nameResId)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            contributions.take(6).forEach { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(item.muscleGroup.nameResId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${item.volumeKg.formatCompact()} kg (${"%.0f".format(item.percentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopExercisesCard(
    exercises: List<TopExerciseMonthly>,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Top Ejercicios del Mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            exercises.forEachIndexed { index, exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (index) {
                                    0 -> Color(0xFFFFB300).copy(alpha = 0.2f)
                                    1 -> Color(0xFFB0BEC5).copy(alpha = 0.2f)
                                    2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (index) {
                                0 -> Color(0xFFFFB300)
                                1 -> Color(0xFF90A4AE)
                                2 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${exercise.totalSets} series · Peso máx: ${"%.1f".format(exercise.maxWeightKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${exercise.totalVolumeKg.formatCompact()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "volumen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
