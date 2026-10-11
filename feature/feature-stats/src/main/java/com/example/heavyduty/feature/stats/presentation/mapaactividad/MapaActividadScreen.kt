package com.example.heavyduty.feature.stats.presentation.mapaactividad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.feature.stats.domain.usecase.ActivityHeatmapStatsReport
import com.example.heavyduty.feature.stats.domain.usecase.TimeSlot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaActividadScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MapaActividadViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_activity_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdCardsSkeleton(modifier = Modifier.padding(innerPadding).padding(16.dp), heights = listOf(180.dp, 200.dp, 220.dp))
            return@Scaffold
        }
        if (uiState.loadFailed) {
            HdErrorState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        val report = uiState.report

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card: Rachas y Constancia
            item {
                StreaksHeroCard(report = report)
            }

            // Frequency by week, month or year
            uiState.frequency?.let { frequency ->
                item {
                    FrequencyCard(panel = frequency, onEvent = viewModel::onEvent)
                }
            }

            // Day of week distribution
            item {
                DayOfWeekDistributionCard(report = report)
            }

            // Time slot distribution
            item {
                TimeSlotDistributionCard(report = report)
            }

            // Summary metrics
            item {
                OverviewMetricsCard(report = report)
            }
        }
    }
}

@Composable
private fun StreaksHeroCard(report: ActivityHeatmapStatsReport) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF7043),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.activity_streaks),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StreakFigure(
                    label = stringResource(R.string.activity_current_streak),
                    value = pluralStringResource(R.plurals.activity_days, report.currentStreakDays, report.currentStreakDays),
                    record = pluralStringResource(R.plurals.activity_record_days, report.longestStreakDays, report.longestStreakDays),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StreakFigure(
                    label = stringResource(R.string.activity_active_weeks),
                    value = pluralStringResource(R.plurals.activity_weeks, report.currentStreakWeeks, report.currentStreakWeeks),
                    record = pluralStringResource(R.plurals.activity_record_weeks, report.longestStreakWeeks, report.longestStreakWeeks),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            StreakTotals(report = report)
        }
    }
}

/** A streak now, big, with its record under it. */
@Composable
private fun StreakFigure(label: String, value: String, record: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = record, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Days trained in the last year, and the share of the year they are. */
@Composable
private fun StreakTotals(report: ActivityHeatmapStatsReport, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                text = stringResource(R.string.activity_days_trained),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pluralStringResource(R.plurals.activity_days, report.totalDaysTrained, report.totalDaysTrained),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.activity_yearly_consistency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${report.consistencyPercentage.formatOneDecimal()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DayOfWeekDistributionCard(report: ActivityHeatmapStatsReport) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DistributionHeader(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.activity_weekly_distribution),
                badge = report.favoriteDayOfWeek?.let { stringResource(R.string.activity_favorite, weekdayName(it)) },
                badgeColor = MaterialTheme.colorScheme.primaryContainer,
                onBadgeColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.dayOfWeekDistribution.forEach { stat ->
                    val isFavorite = stat.dayOfWeek == report.favoriteDayOfWeek
                    DistributionRow(
                        count = stat.count,
                        percentage = stat.percentage,
                        color = MaterialTheme.colorScheme.primary,
                        isFavorite = isFavorite
                    ) {
                        Text(
                            text = weekdayName(stat.dayOfWeek),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            // Room for the longest weekday name ("Wednesday", "Miércoles") plus a gap.
                            modifier = Modifier.width(108.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icon + title, and a "favourite" badge that sits beside the title when it fits and wraps
 * below it whole when it doesn't (large text squeezed it into a two-line block).
 */
@Composable
private fun DistributionHeader(icon: ImageVector, title: String, badge: String?, badgeColor: Color, onBadgeColor: Color) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = onBadgeColor,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TimeSlotDistributionCard(report: ActivityHeatmapStatsReport) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DistributionHeader(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.activity_time_slots),
                badge = report.favoriteTimeSlot?.let { stringResource(R.string.activity_usual, stringResource(it.nameRes)) },
                badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                onBadgeColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.timeSlotDistribution.forEach { slot ->
                    val isFavorite = slot.slot == report.favoriteTimeSlot
                    DistributionRow(
                        count = slot.count,
                        percentage = slot.percentage,
                        color = MaterialTheme.colorScheme.secondary,
                        isFavorite = isFavorite
                    ) {
                        Column(modifier = Modifier.width(115.dp)) {
                            Text(
                                text = stringResource(slot.slot.nameRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%02d:00 - %02d:00".format(slot.slot.startHour, slot.slot.endHour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** [label], then a bar of [percentage] (full colour for the favourite), then "count (pct%)". */
@Composable
private fun DistributionRow(
    count: Int,
    percentage: Float,
    color: Color,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        label()
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isFavorite) color else color.copy(alpha = 0.6f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count (${percentage.roundToInt()}%)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            // A minimum, not a fixed width: with large text "1 (100%)" broke over two lines.
            modifier = Modifier.widthIn(min = 64.dp)
        )
    }
}

@Composable
private fun OverviewMetricsCard(report: ActivityHeatmapStatsReport) {
    HdCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewMetric(
                Icons.Default.Timer,
                "${report.totalHoursTrained.formatOneDecimal()} h",
                stringResource(R.string.activity_total_hours)
            )
            OverviewMetric(
                Icons.Default.Schedule,
                stringResource(R.string.duration_minutes, report.avgMinutesPerSession),
                stringResource(R.string.activity_avg_session)
            )
            OverviewMetric(
                Icons.Default.DateRange,
                "${report.activeWeeksCount}/${report.totalWeeksInWindow}",
                stringResource(R.string.activity_active_weeks)
            )
        }
    }
}

@Composable
private fun OverviewMetric(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Full weekday name in the app's language for a java.util.Calendar day (SUNDAY = 1). */
@Composable
@ReadOnlyComposable
private fun weekdayName(calendarDay: Int): String {
    val isoDay = if (calendarDay == java.util.Calendar.SUNDAY) 7 else calendarDay - 1
    val locale = LocalConfiguration.current.locales[0]
    return java.time.DayOfWeek.of(isoDay)
        .getDisplayName(java.time.format.TextStyle.FULL, locale)
        .replaceFirstChar { it.titlecase(locale) }
}

private val TimeSlot.nameRes: Int
    get() = when (this) {
        TimeSlot.MORNING -> R.string.slot_morning
        TimeSlot.AFTERNOON -> R.string.slot_afternoon
        TimeSlot.EVENING -> R.string.slot_evening
        TimeSlot.NIGHT -> R.string.slot_night
    }
