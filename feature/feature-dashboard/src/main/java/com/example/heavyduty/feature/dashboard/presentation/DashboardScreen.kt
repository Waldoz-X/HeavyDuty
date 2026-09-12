package com.example.heavyduty.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.extensions.formatCompact
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdLineChart
import com.example.heavyduty.core.ui.components.HdTextButton
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.core.ui.theme.GradientCyanEnd
import com.example.heavyduty.core.ui.theme.GradientCyanStart
import com.example.heavyduty.feature.dashboard.domain.model.DayPlanUi
import com.example.heavyduty.feature.dashboard.domain.model.DayStatus

@Composable
fun DashboardScreen(
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DashboardUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
                is DashboardUiEffect.NavigateToRoutines -> onNavigateToRoutines()
                is DashboardUiEffect.NavigateToRoutine -> onStartRoutine(effect.routineId)
                is DashboardUiEffect.ShowError -> { /* Handle Error */ }
            }
        }
    }

    DashboardContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "¡Bienvenido de nuevo, Atleta!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vamos a lograr tus metas hoy.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        WeekStripCard(uiState = uiState, onEvent = onEvent, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        TodayCard(uiState = uiState, onEvent = onEvent, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        BodyWeightCard(uiState = uiState, onEvent = onEvent, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        StreakCard(uiState = uiState, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workouts this week
            StatCard(
                title = "Esta Semana",
                value = "${uiState.weeklyWorkoutsCount}",
                subtitle = "Entrenamientos",
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.weight(1f)
            )

            // Time trained this week
            val minutes = uiState.totalTimeSeconds / 60
            StatCard(
                title = "Tiempo Total",
                value = "$minutes",
                subtitle = "Minutos",
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly volume
        StatCard(
            title = "Volumen Semanal",
            value = "${uiState.weeklyVolumeKg.formatCompact()} kg",
            subtitle = "Kg Levantados",
            icon = Icons.Default.MonitorWeight,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Muscle Distribution Radar Chart
        if (uiState.weeklyMuscleVolume.isNotEmpty() && uiState.weeklyMuscleVolume.size >= 3) {
            Text(
                text = "Distribución Muscular",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            HdCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Map the MuscleGroup enum to localized display strings
                val chartData = uiState.weeklyMuscleVolume
                    .mapKeys { stringResource(it.key.nameResId) }
                com.example.heavyduty.core.ui.components.HdRadarChart(
                    data = chartData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Quick Actions
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Gradient CTA Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(24.dp)),
            onClick = { onEvent(DashboardUiEvent.OnStartFreeWorkout) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientCyanStart, GradientCyanEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Iniciar Entrenamiento Libre",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HdTonalButton(
            text = "Elegir Rutina",
            onClick = { onEvent(DashboardUiEvent.OnChooseRoutine) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            icon = Icons.AutoMirrored.Filled.DirectionsRun
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Activity
        if (uiState.recentWorkoutName != null) {
            Text(
                text = "Actividad Reciente",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            HdCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.recentWorkoutName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uiState.recentWorkoutTime ?: "Recientemente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStripCard(
    uiState: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onEvent(DashboardUiEvent.OnWeekPrev) }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
            }
            Text(
                text = weekOffsetLabel(uiState.weekOffset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { onEvent(DashboardUiEvent.OnWeekNext) }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            uiState.weekPlan.forEach { day -> DayColumn(day = day) }
        }
    }
}

private fun weekOffsetLabel(offset: Int): String = when {
    offset == 0 -> "Esta semana"
    offset == -1 -> "Semana pasada"
    offset == 1 -> "Próxima semana"
    offset < 0 -> "Hace ${-offset} semanas"
    else -> "En $offset semanas"
}

@Composable
private fun DayColumn(day: DayPlanUi, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = day.dayLetter,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (day.isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (day.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val dotColor = when (day.status) {
            DayStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            DayStatus.MISSED -> MaterialTheme.colorScheme.error
            DayStatus.REST -> Color.Transparent
            DayStatus.TODAY_PENDING, DayStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

@Composable
private fun TodayCard(
    uiState: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = uiState.weekPlan.find { it.isToday } ?: return
    HdCard(modifier = modifier) {
        Column {
            Text(
                text = "HOY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (today.routines.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Día de descanso",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                // A day can have more than one routine scheduled (e.g. AM + PM) — each gets
                // its own row so finishing one doesn't hide the option to start the others.
                today.routines.forEachIndexed { index, planned ->
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = planned.routineName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (planned.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completado",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            HdButton(
                                text = "Comenzar",
                                onClick = { onEvent(DashboardUiEvent.OnStartTodayRoutine(planned.routineId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyWeightCard(
    uiState: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.bodyWeightSummary
    HdCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Peso corporal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HdTextButton(text = "+ Registrar", onClick = { onEvent(DashboardUiEvent.OnOpenLogWeight) })
        }

        if (summary.currentKg != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.1f".format(summary.currentKg),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (summary.deltaKg != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val deltaColor = if (summary.deltaKg <= 0f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    val sign = if (summary.deltaKg >= 0f) "+" else ""
                    Text(
                        text = "$sign${"%.1f".format(summary.deltaKg)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = deltaColor,
                    )
                }
            }
            if (summary.goalKg != null) {
                Text(
                    text = "Meta: ${"%.1f".format(summary.goalKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HdLineChart(
                data = summary.chartPoints,
                goalValue = summary.goalKg,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registra tu peso para ver la tendencia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (uiState.showLogWeightSheet) {
        LogWeightBottomSheet(
            onDismiss = { onEvent(DashboardUiEvent.OnDismissLogWeight) },
            onSave = { kg -> onEvent(DashboardUiEvent.OnLogWeight(kg)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogWeightBottomSheet(
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
) {
    var weightInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val weightKg = weightInput.toFloatOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Registrar peso",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("Peso (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Spacer(modifier = Modifier.height(24.dp))
            HdButton(
                text = "Guardar",
                onClick = { weightKg?.let(onSave) },
                enabled = weightKg != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StreakCard(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${uiState.streakDays} días seguidos",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = if (uiState.plannedDaysThisWeek > 0) {
                    "${uiState.weeklyWorkoutsCount}/${uiState.plannedDaysThisWeek} esta semana"
                } else {
                    "${uiState.weeklyWorkoutsCount} entrenamientos esta semana"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
