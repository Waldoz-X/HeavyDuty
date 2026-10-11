package com.example.heavyduty.feature.stats.presentation.mapaactividad

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.theme.raised
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** From here up a cell is dark enough to need light text. */
private const val LIGHT_TEXT_INTENSITY = 0.6f

/** Days that haven't happened yet are drawn faint. */
private const val FUTURE_ALPHA = 0.35f

/** A trained day's bar never shrinks below this share of the track, so a short day still shows. */
private const val MIN_BAR_FRACTION = 0.06f

private const val MINUTES_PER_HOUR = 60

/** From ten hours a decimal adds nothing to a bar label. */
private const val WHOLE_HOURS_FROM = 10f

/**
 * Training frequency by week (minutes per day), month (a calendar shaded by effort) or year
 * (days trained per month), with arrows to walk back through the history.
 */
@Composable
internal fun FrequencyCard(panel: FrequencyPanel, onEvent: (MapaActividadUiEvent) -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.activity_frequency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            FrequencyViewSelector(selected = panel.view, onSelect = { onEvent(MapaActividadUiEvent.OnSelectFrequencyView(it)) })
            Spacer(modifier = Modifier.height(8.dp))
            PeriodNavigator(panel = panel, onEvent = onEvent)
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedContent(
                targetState = panel,
                contentKey = { it.view to it.periodStart },
                transitionSpec = { periodTransition() },
                label = "frequency"
            ) { shown ->
                when (val content = shown.content) {
                    is FrequencyContent.Week -> WeekBars(content)
                    is FrequencyContent.Month -> MonthCalendar(content)
                    is FrequencyContent.Year -> YearBars(content)
                }
            }
        }
    }
}

/** Later periods slide in from the right, earlier ones from the left; a new view just fades in. */
private fun AnimatedContentTransitionScope<FrequencyPanel>.periodTransition(): ContentTransform {
    if (initialState.view != targetState.view) return fadeIn() togetherWith fadeOut()
    val forward = targetState.periodStart > initialState.periodStart
    return (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
        (slideOutHorizontally { if (forward) -it else it } + fadeOut())
}

@Composable
private fun FrequencyViewSelector(selected: FrequencyView, onSelect: (FrequencyView) -> Unit) {
    val views = FrequencyView.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        views.forEachIndexed { index, view ->
            SegmentedButton(
                selected = view == selected,
                onClick = { onSelect(view) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = views.size)
            ) {
                Text(stringResource(view.labelRes))
            }
        }
    }
}

private val FrequencyView.labelRes: Int
    get() = when (this) {
        FrequencyView.WEEK -> R.string.frequency_view_week
        FrequencyView.MONTH -> R.string.frequency_view_month
        FrequencyView.YEAR -> R.string.frequency_view_year
    }

/** ‹ Octubre 2026 › with the period's totals underneath. */
@Composable
private fun PeriodNavigator(panel: FrequencyPanel, onEvent: (MapaActividadUiEvent) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onEvent(MapaActividadUiEvent.OnPreviousPeriod) }, enabled = panel.canGoBack) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.frequency_previous))
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = periodTitle(panel),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = periodSummary(panel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = { onEvent(MapaActividadUiEvent.OnNextPeriod) }, enabled = panel.canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.frequency_next))
        }
    }
}

@Composable
private fun periodTitle(panel: FrequencyPanel): String {
    val locale = LocalConfiguration.current.locales[0]
    val start = Date(panel.periodStart)
    return when (panel.view) {
        FrequencyView.WEEK -> {
            val lastDay = Date(panel.periodEnd - 1)
            "${SimpleDateFormat("d MMM", locale).format(start)} – ${SimpleDateFormat("d MMM yyyy", locale).format(lastDay)}"
        }

        FrequencyView.MONTH -> SimpleDateFormat("LLLL yyyy", locale).format(start).replaceFirstChar { it.titlecase(locale) }

        FrequencyView.YEAR -> SimpleDateFormat("yyyy", locale).format(start)
    }
}

/** "3 entrenamientos · 2 h 15 min · 3 días", or a plain "no workouts" line. */
@Composable
private fun periodSummary(panel: FrequencyPanel): String {
    if (panel.sessions == 0) return stringResource(R.string.frequency_empty)
    return listOf(
        pluralStringResource(R.plurals.frequency_sessions, panel.sessions, panel.sessions),
        formatMinutes(panel.minutes),
        pluralStringResource(R.plurals.activity_days, panel.trainedDays, panel.trainedDays)
    ).joinToString(" · ")
}

@Composable
private fun formatMinutes(minutes: Float): String {
    val total = minutes.roundToInt()
    return if (total < MINUTES_PER_HOUR) {
        stringResource(R.string.duration_minutes, total)
    } else {
        stringResource(R.string.duration_hours_minutes, total / MINUTES_PER_HOUR, total % MINUTES_PER_HOUR)
    }
}

// ------------------------------------------------------------------------------------ week

@Composable
private fun WeekBars(week: FrequencyContent.Week) {
    val locale = LocalConfiguration.current.locales[0]
    val longest = week.days.maxOf { it.minutes }
    Column {
        Caption(stringResource(R.string.frequency_week_caption))
        Row(modifier = Modifier.fillMaxWidth()) {
            week.days.forEachIndexed { index, day ->
                BarColumn(
                    description = "${DayOfWeek.of(index + 1).getDisplayName(TextStyle.FULL, locale)} ${day.dayOfMonth}: " +
                        if (day.minutes > 0f) shortDuration(day.minutes) else stringResource(R.string.a11y_rest_day),
                    value = if (day.minutes > 0f) shortDuration(day.minutes) else "",
                    fraction = barFraction(day.minutes, longest),
                    label = weekdayLabel(DayOfWeek.of(index + 1), locale),
                    sublabel = day.dayOfMonth.toString(),
                    highlighted = day.isToday,
                    faded = day.isFuture,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** "Lu", "Ma", "Mi"… — the one-letter forms repeat ("m, m") and come out lowercase in Spanish. */
private fun weekdayLabel(day: DayOfWeek, locale: Locale): String = day.getDisplayName(TextStyle.SHORT, locale)
    .take(2)
    .replaceFirstChar { it.titlecase(locale) }

/** Fits over a narrow bar: "45 min" under an hour, then hours — "1.2 h", "35 h". */
@Composable
private fun shortDuration(minutes: Float): String {
    val hours = minutes / MINUTES_PER_HOUR
    return when {
        minutes < MINUTES_PER_HOUR -> stringResource(R.string.duration_minutes, minutes.roundToInt())
        hours < WHOLE_HOURS_FROM -> stringResource(R.string.frequency_hours, hours.formatOneDecimal())
        else -> stringResource(R.string.frequency_hours, hours.roundToInt().toString())
    }
}

// ------------------------------------------------------------------------------------ month

@Composable
private fun MonthCalendar(month: FrequencyContent.Month) {
    val locale = LocalConfiguration.current.locales[0]
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { weekday ->
                Text(
                    text = weekdayLabel(weekday, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        month.weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp)
                    ) {
                        if (day != null) DayCell(day)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        IntensityLegend()
    }
}

@Composable
private fun DayCell(day: FrequencyDay) {
    val colors = MaterialTheme.colorScheme
    val fill = if (day.intensity > 0f) lerp(colors.raised, colors.primary, day.intensity) else colors.raised
    val shape = RoundedCornerShape(10.dp)
    val description = dayDescription(day)
    Box(
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = description }
            .fillMaxHeight()
            .fillMaxWidth()
            .alpha(if (day.isFuture) FUTURE_ALPHA else 1f)
            .clip(shape)
            .background(fill)
            .then(if (day.isToday) Modifier.border(2.dp, colors.primary, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (day.intensity > 0f || day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.intensity >= LIGHT_TEXT_INTENSITY) colors.onPrimary else colors.onSurface
        )
    }
}

/** "9 octubre: 1 entrenamiento, 35 h", "8 octubre: descanso", or just the date for days still ahead. */
@Composable
private fun dayDescription(day: FrequencyDay): String {
    val locale = LocalConfiguration.current.locales[0]
    val date = SimpleDateFormat("d MMMM", locale).format(Date(day.dayStart))
    return when {
        day.isFuture -> date
        day.sessions == 0 -> "$date: ${stringResource(R.string.a11y_rest_day)}"
        else -> "$date: ${pluralStringResource(R.plurals.frequency_sessions, day.sessions, day.sessions)}, ${shortDuration(day.minutes)}"
    }
}

/** Menos ■■■■■ Más — what the month calendar's shades mean. */
@Composable
private fun IntensityLegend() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.activity_less), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Spacer(modifier = Modifier.width(6.dp))
        listOf(0f, MIN_TRAINED_INTENSITY, 0.5f, 0.75f, 1f).forEach { level ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(11.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(lerp(colors.raised, colors.primary, level))
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.activity_more), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------------------------ year

@Composable
private fun YearBars(year: FrequencyContent.Year) {
    val locale = LocalConfiguration.current.locales[0]
    val most = year.months.maxOf { it.trainedDays }.toFloat()
    Column {
        Caption(stringResource(R.string.frequency_year_caption))
        Row(modifier = Modifier.fillMaxWidth()) {
            year.months.forEach { month ->
                BarColumn(
                    description = Month.of(monthNumber(month.monthStart)).getDisplayName(TextStyle.FULL_STANDALONE, locale)
                        .replaceFirstChar { it.titlecase(locale) } + ": " +
                        pluralStringResource(R.plurals.activity_days, month.trainedDays, month.trainedDays),
                    value = if (month.trainedDays > 0) month.trainedDays.toString() else "",
                    fraction = barFraction(month.trainedDays.toFloat(), most),
                    label = Month.of(monthNumber(month.monthStart)).getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    sublabel = null,
                    highlighted = month.isCurrent,
                    faded = month.isFuture,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun monthNumber(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MONTH) + 1

// ------------------------------------------------------------------------------------ shared

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

private fun barFraction(value: Float, largest: Float): Float = when {
    value <= 0f || largest <= 0f -> 0f
    else -> (value / largest).coerceIn(MIN_BAR_FRACTION, 1f)
}

/** A value on top, a rounded bar growing from the bottom of its track, and the label(s) under it. */
@Composable
private fun BarColumn(
    value: String,
    fraction: Float,
    label: String,
    sublabel: String?,
    highlighted: Boolean,
    faded: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val animated by animateFloatAsState(targetValue = fraction, label = "bar")
    Column(
        // Read as one sentence ("Viernes 9: 35 h"), not as a number, a letter and a date apart.
        modifier = modifier
            .alpha(if (faded) FUTURE_ALPHA else 1f)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.raised),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (animated > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animated)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (highlighted) colors.primary else colors.primary.copy(alpha = 0.7f))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) colors.primary else colors.onSurfaceVariant,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
        if (sublabel != null) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (highlighted) colors.primary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlighted) colors.onPrimary else colors.onSurface
                )
            }
        }
    }
}
