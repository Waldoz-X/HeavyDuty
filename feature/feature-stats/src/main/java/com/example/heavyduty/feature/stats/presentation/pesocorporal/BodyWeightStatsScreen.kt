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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import com.example.heavyduty.core.common.extensions.toFormattedDateTime
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.domain.model.toKgFrom
import com.example.heavyduty.core.ui.LocalWeightUnit
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdLineChart
import com.example.heavyduty.core.ui.formatWeight
import com.example.heavyduty.core.ui.formatWeightValue
import com.example.heavyduty.core.ui.weightUnitLabel
import com.example.heavyduty.feature.stats.domain.usecase.BodyWeightHistoryItem
import com.example.heavyduty.feature.stats.domain.usecase.BodyWeightStatsReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightStatsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyWeightStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<BodyWeightHistoryItem?>(null) }

    Scaffold(
        topBar = { BodyWeightTopBar(onNavigateBack = onNavigateBack, onSetGoal = { showGoalDialog = true }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddWeightDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.bw_log_weight)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BodyWeightBody(
            uiState = uiState,
            onLogWeight = { showAddWeightDialog = true },
            onSetGoal = { showGoalDialog = true },
            onDelete = { entryToDelete = it },
            modifier = Modifier.padding(innerPadding)
        )
    }

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

    entryToDelete?.let { entry ->
        DeleteEntryDialog(
            entry = entry,
            onConfirm = {
                viewModel.deleteWeight(entry)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }
}

/** Goal, chart, min/max/average and the history of weigh-ins. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyWeightTopBar(onNavigateBack: () -> Unit, onSetGoal: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.stats_bodyweight_title)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        actions = {
            IconButton(onClick = onSetGoal) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = stringResource(R.string.bw_set_goal),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

/** Loading, failed, nothing logged yet — or the goal, chart, figures and history. */
@Composable
private fun BodyWeightBody(
    uiState: BodyWeightStatsUiState,
    onLogWeight: () -> Unit,
    onSetGoal: () -> Unit,
    onDelete: (BodyWeightHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val report = uiState.report
    when {
        uiState.isLoading -> HdCardsSkeleton(modifier = modifier.padding(16.dp), heights = listOf(160.dp, 240.dp, 200.dp))

        uiState.loadFailed -> HdErrorState(modifier = modifier)

        report == null || report.chartPoints.isEmpty() -> BodyWeightEmptyState(
            onLogWeight = onLogWeight,
            onSetGoal = onSetGoal,
            modifier = modifier
        )

        else -> BodyWeightContent(report = report, onEditGoal = onSetGoal, onDelete = onDelete, modifier = modifier)
    }
}

@Composable
private fun BodyWeightContent(
    report: BodyWeightStatsReport,
    onEditGoal: () -> Unit,
    onDelete: (BodyWeightHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            WeightGoalHeroCard(
                report = report,
                onEditGoal = onEditGoal
            )
        }

        item {
            WeightChartCard(report = report)
        }

        item {
            WeightMetricsRow(report = report)
        }

        item {
            HistoryHeader(entryCount = report.history.size)
        }

        items(report.history, key = { it.id }) { entry ->
            HistoryEntryCard(
                entry = entry,
                onDelete = { onDelete(entry) }
            )
        }
    }
}

/** No weigh-ins yet: what this screen is for, and the two ways to start. */
@Composable
private fun BodyWeightEmptyState(onLogWeight: () -> Unit, onSetGoal: () -> Unit, modifier: Modifier = Modifier) {
    // The buttons go in HdEmptyState's action slot: placed after it in a Column they were pushed
    // off-screen, since the empty state fills all the height it's given.
    HdEmptyState(
        icon = Icons.Default.MonitorWeight,
        title = stringResource(R.string.bw_empty_title),
        description = stringResource(R.string.bw_empty_desc),
        modifier = modifier.padding(16.dp),
        action = {
            // Stacked: side by side they don't fit a phone's width in either language.
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLogWeight) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bw_log_first))
                }
                OutlinedButton(onClick = onSetGoal) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bw_fix_goal))
                }
            }
        }
    )
}

/** Weight over time, with the goal drawn as a line when there is one. */
@Composable
private fun WeightChartCard(report: BodyWeightStatsReport) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.bw_over_time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (report.goalWeightKg != null) {
                Text(
                    text = stringResource(R.string.bw_goal_value, report.goalWeightKg.formatWeight()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val unit = LocalWeightUnit.current
        HdLineChart(
            data = report.chartPoints.map { (time, kg) -> time to kg.kgTo(unit) },
            goalValue = report.goalWeightKg?.kgTo(unit),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Lowest, highest and average weight. */
@Composable
private fun WeightMetricsRow(report: BodyWeightStatsReport) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = stringResource(R.string.metric_min),
            value = report.minWeightKg,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = stringResource(R.string.metric_max),
            value = report.maxWeightKg,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = stringResource(R.string.bw_average),
            value = report.avgWeightKg,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoryHeader(entryCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.bw_history),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = pluralStringResource(R.plurals.bw_entries, entryCount, entryCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeleteEntryDialog(entry: BodyWeightHistoryItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bw_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.bw_delete_message,
                    entry.weightKg.formatWeight(),
                    entry.loggedAt.toFormattedDateTime()
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun WeightGoalHeroCard(report: BodyWeightStatsReport, onEditGoal: () -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        GoalCardHeader(onEditGoal = onEditGoal)

        Spacer(modifier = Modifier.height(12.dp))

        val goalKg = report.goalWeightKg
        val currentKg = report.currentWeightKg
        if (goalKg != null && currentKg != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                WeightFigure(
                    label = stringResource(R.string.bw_current),
                    weightKg = currentKg,
                    color = MaterialTheme.colorScheme.onSurface,
                    unitColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WeightFigure(
                    label = stringResource(R.string.bw_target),
                    weightKg = goalKg,
                    color = MaterialTheme.colorScheme.primary,
                    unitColor = MaterialTheme.colorScheme.primary,
                    alignment = Alignment.End
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GoalProgress(progress = report.progressPercentage ?: 0f, remainingKg = report.remainingToGoalKg ?: 0f)

            report.startWeightKg?.let { startKg ->
                Spacer(modifier = Modifier.height(12.dp))
                StartAndChangeRow(startKg = startKg, currentKg = currentKg)
            }
        } else {
            NoGoalPrompt(onDefineGoal = onEditGoal)
        }
    }
}

@Composable
private fun GoalCardHeader(onEditGoal: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.bw_goal_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onEditGoal) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.bw_edit_goal),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A labelled weight with its unit, e.g. "Actual 82.5 kg". */
@Composable
private fun WeightFigure(
    label: String,
    weightKg: Float,
    color: Color,
    unitColor: Color,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = weightKg.formatWeightValue(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = weightUnitLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = unitColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/** How far along the goal is: what's left (or past it) and a progress bar. */
@Composable
private fun GoalProgress(progress: Float, remainingKg: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val statusText = if (remainingKg == 0f) {
                stringResource(R.string.bw_goal_reached)
            } else if (remainingKg > 0) {
                stringResource(R.string.bw_remaining, remainingKg.formatWeight())
            } else {
                stringResource(R.string.bw_over, (-remainingKg).formatWeight())
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/** Starting weight and the total change since then. */
@Composable
private fun StartAndChangeRow(startKg: Float, currentKg: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.bw_start, startKg.formatWeight()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val totalChange = currentKg - startKg
        val sign = if (totalChange > 0) "+" else ""
        Text(
            text = stringResource(R.string.bw_total_change, "$sign${totalChange.formatWeight()}"),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (totalChange <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NoGoalPrompt(onDefineGoal: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.bw_no_goal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onDefineGoal) {
            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.bw_define_goal))
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: BodyWeightHistoryItem, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.loggedAt.toFormattedDateTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.deltaKg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    WeightTrend(deltaKg = entry.deltaKg)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.weightKg.formatWeightValue(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = weightUnitLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** The change since the weigh-in before: up in orange, down in green, about the same in grey. */
@Composable
private fun WeightTrend(deltaKg: Float, modifier: Modifier = Modifier) {
    val (icon, color) = when {
        deltaKg > 0.05f -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFFFF9800)
        deltaKg < -0.05f -> Icons.AutoMirrored.Filled.TrendingDown to Color(0xFF4CAF50)
        else -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val sign = if (deltaKg > 0) "+" else ""
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$sign${deltaKg.formatWeight()}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetricCard(title: String, value: Float?, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (value != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value.formatWeightValue(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = weightUnitLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "--",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalWeightDialog(currentGoal: Float?, onDismiss: () -> Unit, onSave: (Float?) -> Unit) {
    val unit = LocalWeightUnit.current
    var textValue by remember { mutableStateOf(currentGoal?.kgTo(unit)?.formatOneDecimal() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.bw_goal_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.bw_goal_hint, unit.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.bw_goal_label, unit.label)) },
                    placeholder = { Text(stringResource(R.string.bw_goal_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.replace(',', '.').toFloatOrNull()
                    onSave(parsed?.toKgFrom(unit))
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (currentGoal != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Text(stringResource(R.string.bw_remove_goal), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

@Composable
private fun AddWeightDialog(initialWeight: Float?, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    val unit = LocalWeightUnit.current
    var textValue by remember { mutableStateOf(initialWeight?.kgTo(unit)?.formatOneDecimal() ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.bw_log_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.bw_log_hint, unit.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.bw_weight_label, unit.label)) },
                    placeholder = { Text(stringResource(R.string.bw_weight_placeholder)) },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.bw_invalid)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.replace(',', '.').toFloatOrNull()
                    if (parsed != null && parsed > 0) {
                        onSave(parsed.toKgFrom(unit))
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.action_register))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
