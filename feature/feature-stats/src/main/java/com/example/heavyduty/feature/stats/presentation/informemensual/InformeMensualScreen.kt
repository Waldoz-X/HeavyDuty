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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatCompact
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import com.example.heavyduty.core.domain.model.kgTo
import com.example.heavyduty.core.ui.LocalWeightUnit
import com.example.heavyduty.core.ui.components.HdBarChart
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdStatCard
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.core.ui.formatWeight
import com.example.heavyduty.core.ui.nameResId
import com.example.heavyduty.core.ui.weightUnitLabel
import com.example.heavyduty.feature.stats.domain.model.MonthlyReport
import com.example.heavyduty.feature.stats.domain.model.MuscleVolumeContribution
import com.example.heavyduty.feature.stats.domain.model.TopExerciseMonthly
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformeMensualScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: InformeMensualViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_monthly_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cabecera interactiva con selector de meses (< Mes Año >)
            MonthSelectorHeader(
                monthLabel = monthName(uiState.report.monthStartMillis),
                hasNextMonth = uiState.hasNextMonth,
                onPreviousMonth = { viewModel.selectPreviousMonth() },
                onNextMonth = { viewModel.selectNextMonth() }
            )

            MonthlyReportBody(uiState = uiState)
        }
    }
}

/** Loading, failed, a month without workouts — or its report. */
@Composable
private fun MonthlyReportBody(uiState: InformeMensualUiState) {
    when {
        uiState.isLoading -> HdCardsSkeleton(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            heights = listOf(56.dp, 120.dp, 120.dp, 220.dp)
        )

        uiState.loadFailed -> HdErrorState(modifier = Modifier.fillMaxSize())

        uiState.report.totalWorkouts == 0 -> MonthlyEmptyState(monthStartMillis = uiState.report.monthStartMillis)

        else -> MonthlyReportList(report = uiState.report)
    }
}

@Composable
private fun MonthlyReportList(report: MonthlyReport, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MonthlyStatsGrid(report = report) }
        if (report.muscleContributions.isNotEmpty()) {
            item { MuscleDistributionCard(contributions = report.muscleContributions, topMuscle = report.topMuscleGroup) }
        }
        if (report.topExercises.isNotEmpty()) {
            item { TopExercisesCard(exercises = report.topExercises) }
        }
        if (report.workoutsPerWeek.isNotEmpty()) {
            item {
                WeeklyBarChartCard(
                    title = stringResource(R.string.monthly_workouts_per_week),
                    data = report.workoutsPerWeek.map { (week, count) ->
                        stringResource(R.string.monthly_week_label, week) to
                            count.toFloat()
                    },
                    valueFormatter = { it.roundToInt().toString() }
                )
            }
        }
        if (report.volumePerWeek.isNotEmpty()) {
            item {
                val unit = LocalWeightUnit.current
                WeeklyBarChartCard(
                    title = stringResource(R.string.monthly_volume_per_week, weightUnitLabel()),
                    data = report.volumePerWeek.map { (week, kg) -> stringResource(R.string.monthly_week_label, week) to kg.kgTo(unit) },
                    // Already in the user's unit, which the title names: "1.3k".
                    valueFormatter = { it.formatCompact() }
                )
            }
        }
    }
}

/** A month with no finished workouts. */
@Composable
private fun MonthlyEmptyState(monthStartMillis: Long) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        HdEmptyState(
            icon = Icons.Default.FitnessCenter,
            title = stringResource(R.string.monthly_empty_title),
            description = stringResource(R.string.monthly_empty_desc, monthName(monthStartMillis).lowercase())
        )
    }
}

/** Volume, workouts, time and sets, with the change against last month where it applies. */
@Composable
private fun MonthlyStatsGrid(report: MonthlyReport) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VolumeAndWorkouts(report = report)
        TimeAndSets(report = report)
    }
}

@Composable
private fun VolumeAndWorkouts(report: MonthlyReport, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HdStatCard(
            icon = Icons.Default.FitnessCenter,
            label = stringResource(R.string.monthly_total_volume),
            value = report.totalVolumeKg.formatTotalWeight(),
            deltaText = report.prevMonthVolumeDeltaPercent?.let {
                val sign = if (it >= 0) "+" else ""
                stringResource(R.string.monthly_vs_last_month, "$sign${it.formatOneDecimal()}%")
            },
            isPositive = (report.prevMonthVolumeDeltaPercent ?: 0f) >= 0,
            modifier = Modifier.weight(1f)
        )
        HdStatCard(
            icon = Icons.Default.CalendarMonth,
            label = stringResource(R.string.stats_workouts),
            value = "${report.totalWorkouts}",
            deltaText = report.prevMonthWorkoutsDelta?.let {
                val sign = if (it >= 0) "+" else ""
                stringResource(R.string.monthly_vs_last_month, "$sign$it")
            },
            isPositive = (report.prevMonthWorkoutsDelta ?: 0) >= 0,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeAndSets(report: MonthlyReport, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val hours = report.totalDurationMinutes / 60
        val mins = report.totalDurationMinutes % 60
        HdStatCard(
            icon = Icons.Default.Timer,
            label = stringResource(R.string.monthly_total_time),
            value = if (hours >
                0
            ) {
                stringResource(R.string.duration_hours_minutes, hours, mins)
            } else {
                stringResource(R.string.duration_minutes, mins)
            },
            subtext = stringResource(R.string.monthly_avg_session, report.avgSessionDurationMinutes),
            modifier = Modifier.weight(1f)
        )
        HdStatCard(
            icon = Icons.Default.Repeat,
            label = stringResource(R.string.monthly_effective_sets),
            value = "${report.totalSets}",
            subtext = stringResource(R.string.monthly_sessions_per_week, report.workoutsPerWeekAverage.formatOneDecimal()),
            modifier = Modifier.weight(1f)
        )
    }
}

/** One value per week of the month as bars, each labelled by [valueFormatter]. */
@Composable
private fun WeeklyBarChartCard(title: String, data: List<Pair<String, Float>>, valueFormatter: (Float) -> String) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        HdBarChart(
            data = data,
            valueFormatter = valueFormatter,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
    }
}

@Composable
private fun MonthSelectorHeader(
    monthLabel: String,
    hasNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.monthly_prev_month),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = onNextMonth,
            enabled = hasNextMonth
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.monthly_next_month),
                tint = if (hasNextMonth) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.3f
                    )
                }
            )
        }
    }
}

@Composable
private fun MuscleDistributionCard(
    contributions: List<MuscleVolumeContribution>,
    topMuscle: com.example.heavyduty.core.domain.model.MuscleGroup?,
    modifier: Modifier = Modifier
) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.monthly_muscle_distribution),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (topMuscle != null) {
                Text(
                    text = stringResource(R.string.monthly_top_muscle, stringResource(topMuscle.nameResId)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            contributions.take(6).forEach { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(item.muscleGroup.nameResId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.volumeKg.formatTotalWeight()} (${item.percentage.roundToInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TopExercisesCard(exercises: List<TopExerciseMonthly>, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.monthly_top_exercises),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            exercises.forEachIndexed { index, exercise -> TopExerciseRow(rank = index + 1, exercise = exercise) }
        }
    }
}

@Composable
private fun TopExerciseRow(rank: Int, exercise: TopExerciseMonthly, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RankBadge(rank = rank)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.monthly_exercise_detail,
                    pluralStringResource(R.plurals.stats_set_count, exercise.totalSets, exercise.totalSets),
                    exercise.maxWeightKg.formatWeight()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = exercise.totalVolumeKg.formatTotalWeight(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.monthly_volume_lower),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 1, 2, 3 in gold, silver and bronze; the rest plain. */
@Composable
private fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val medal = when (rank) {
        1 -> Color(0xFFFFB300) to Color(0xFFFFB300)
        2 -> Color(0xFFB0BEC5) to Color(0xFF90A4AE)
        3 -> Color(0xFFCD7F32) to Color(0xFFCD7F32)
        else -> null
    }
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(medal?.first?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = medal?.second ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "Octubre 2026" / "October 2026", in the app's language. */
@Composable
private fun monthName(monthStartMillis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return java.text.SimpleDateFormat("LLLL yyyy", locale)
        .format(java.util.Date(monthStartMillis))
        .replaceFirstChar { it.titlecase(locale) }
}
