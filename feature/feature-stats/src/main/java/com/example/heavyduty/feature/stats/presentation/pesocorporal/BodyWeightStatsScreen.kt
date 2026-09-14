package com.example.heavyduty.feature.stats.presentation.pesocorporal

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.extensions.toFormattedDateTime
import com.example.heavyduty.core.common.extensions.toWeightString
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLineChart
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.feature.stats.domain.usecase.BodyWeightHistoryItem
import com.example.heavyduty.feature.stats.domain.usecase.BodyWeightStatsReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightStatsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyWeightStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<BodyWeightHistoryItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peso Corporal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Definir Meta",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddWeightDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar peso") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdLoadingIndicator(
                modifier = Modifier.padding(innerPadding),
                message = "Cargando historial...",
            )
            return@Scaffold
        }

        val report = uiState.report

        if (report == null || report.chartPoints.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HdEmptyState(
                    icon = Icons.Default.MonitorWeight,
                    title = "Sin registros de peso",
                    description = "Comienza a registrar tu peso diario o semanal para monitorear tu progreso y alcanzar tu meta.",
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showAddWeightDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar primer peso")
                    }
                    OutlinedButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fijar meta")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // 1. Tarjeta Hero de Meta de Peso
                item {
                    WeightGoalHeroCard(
                        report = report,
                        onEditGoal = { showGoalDialog = true },
                    )
                }

                // 2. Gráfica de Progreso Histórico
                item {
                    HdCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Evolución en el Tiempo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (report.goalWeightKg != null) {
                                Text(
                                    text = "Meta: ${"%.1f".format(report.goalWeightKg)} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HdLineChart(
                            data = report.chartPoints,
                            goalValue = report.goalWeightKg,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 3. Tarjetas de métricas estadísticas resumidas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MetricCard(
                            title = "Mínimo",
                            value = report.minWeightKg,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            title = "Máximo",
                            value = report.maxWeightKg,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            title = "Promedio",
                            value = report.avgWeightKg,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 4. Título del Historial
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Historial de Registros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${report.history.size} registros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 5. Lista de Registros con Variación
                items(report.history, key = { it.id }) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        onDelete = { entryToDelete = entry },
                    )
                }
            }
        }
    }

    // Diálogo para definir o editar la meta de peso
    if (showGoalDialog) {
        GoalWeightDialog(
            currentGoal = uiState.report?.goalWeightKg,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                viewModel.updateGoalWeight(newGoal)
                showGoalDialog = false
            }
        )
    }

    // Diálogo para registrar un nuevo peso corporal
    if (showAddWeightDialog) {
        val lastWeight = uiState.report?.currentWeightKg
        AddWeightDialog(
            initialWeight = lastWeight,
            onDismiss = { showAddWeightDialog = false },
            onSave = { weightKg ->
                viewModel.logWeight(weightKg)
                showAddWeightDialog = false
            }
        )
    }

    // Diálogo de confirmación para eliminar registro
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Eliminar registro") },
            text = { Text("¿Deseas eliminar el registro de ${entryToDelete?.weightKg?.toWeightString()} kg del ${entryToDelete?.loggedAt?.toFormattedDateTime()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let { viewModel.deleteWeight(it) }
                        entryToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun WeightGoalHeroCard(
    report: BodyWeightStatsReport,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Meta de Peso Corporal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onEditGoal) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modificar meta",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (report.goalWeightKg != null && report.currentWeightKg != null) {
            val progress = report.progressPercentage ?: 0f
            val remaining = report.remainingToGoalKg ?: 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "Peso Actual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = report.currentWeightKg.toWeightString(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Objetivo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = report.goalWeightKg.toWeightString(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso hacia la meta
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val statusText = if (kotlin.math.abs(remaining) <= 0.2f) {
                        "¡Meta cumplida! 🎉"
                    } else if (remaining > 0) {
                        "Faltan ${"%.1f".format(remaining)} kg"
                    } else {
                        "Exceso: ${"%.1f".format(-remaining)} kg"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            if (report.startWeightKg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Inicio: ${report.startWeightKg.toWeightString()} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val totalChange = report.currentWeightKg - report.startWeightKg
                    val sign = if (totalChange > 0) "+" else ""
                    Text(
                        text = "Cambio total: $sign${"%.1f".format(totalChange)} kg",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (totalChange <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            // No hay meta fijada
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Aún no has definido un peso meta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onEditGoal) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Definir Meta de Peso")
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: BodyWeightHistoryItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = entry.loggedAt.toFormattedDateTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.deltaKg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when {
                            entry.deltaKg > 0.05f -> Icons.AutoMirrored.Filled.TrendingUp
                            entry.deltaKg < -0.05f -> Icons.AutoMirrored.Filled.TrendingDown
                            else -> Icons.AutoMirrored.Filled.TrendingFlat
                        }
                        val color = when {
                            entry.deltaKg > 0.05f -> Color(0xFFFF9800)
                            entry.deltaKg < -0.05f -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val sign = if (entry.deltaKg > 0) "+" else ""
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$sign${"%.1f".format(entry.deltaKg)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.weightKg.toWeightString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: Float?,
    modifier: Modifier = Modifier,
) {
    HdCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (value != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = "--",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalWeightDialog(
    currentGoal: Float?,
    onDismiss: () -> Unit,
    onSave: (Float?) -> Unit,
) {
    var textValue by remember { mutableStateOf(currentGoal?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Meta de Peso Corporal") },
        text = {
            Column {
                Text(
                    text = "Ingresa tu peso objetivo en kilogramos para calcular tu barra de progreso y trazar la meta en la gráfica.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Peso meta (kg)") },
                    placeholder = { Text("ej. 75.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.replace(',', '.').toFloatOrNull()
                    onSave(parsed)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                if (currentGoal != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Quitar meta", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun AddWeightDialog(
    initialWeight: Float?,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
) {
    var textValue by remember { mutableStateOf(initialWeight?.toString() ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Registrar Peso") },
        text = {
            Column {
                Text(
                    text = "Ingresa tu peso medido en kilogramos:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        isError = false
                    },
                    label = { Text("Peso corporal (kg)") },
                    placeholder = { Text("ej. 80.5") },
                    isError = isError,
                    supportingText = if (isError) { { Text("Ingresa un peso numérico válido") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.replace(',', '.').toFloatOrNull()
                    if (parsed != null && parsed > 0) {
                        onSave(parsed)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
