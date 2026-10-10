package com.example.heavyduty.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heavyduty.core.ui.theme.raised
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * A GitHub-style year-of-activity grid: one column per week, one row per weekday, each cell
 * shaded by [data]'s value for that day (0..1). Pure Canvas, no charting library, matching the
 * style of [HdLineChart] / [HdBarChart] / [HdRadarChart].
 *
 * [data] is keyed by local-midnight millis for each day — days with no entry (or a future day
 * within the current week) render as [baseColor].
 */
@Composable
fun HdActivityHeatmap(
    data: Map<Long, Float>,
    modifier: Modifier = Modifier,
    weeks: Int = 53,
    // An empty day must still show against the card it sits in, so one tonal step above it.
    baseColor: Color = MaterialTheme.colorScheme.raised,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cellSpacing: Dp = 2.dp,
    height: Dp = 120.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp)
    val todayMillis = remember { startOfDayMillis(System.currentTimeMillis()) }
    val grid = remember(todayMillis, weeks) { buildWeekGrid(todayMillis, weeks) }
    val topLabelHeight = 16.dp
    val locale = LocalConfiguration.current.locales[0]
    val monthFormat = remember(locale) { SimpleDateFormat("MMM", locale) }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val spacingPx = cellSpacing.toPx()
        val topPx = topLabelHeight.toPx()
        // Cells fill the available width exactly (weeks is fixed); height is whatever the
        // caller reserved, so cells may not be perfectly square at unusual aspect ratios.
        val cellWidthPx = (size.width - (weeks - 1) * spacingPx) / weeks
        val cellHeightPx = (size.height - topPx - 6 * spacingPx) / 7
        val cornerRadius = CornerRadius(2.dp.toPx())

        val labelGapPx = 4.dp.toPx()
        val monthLabels = monthStartColumns(grid).map { (weekIndex, dayMillis) ->
            val text = monthFormat.format(Date(dayMillis)).replaceFirstChar { it.titlecase(locale) }
            val layout = textMeasurer.measure(text, style = labelStyle)
            MonthLabel(x = weekIndex * (cellWidthPx + spacingPx), width = layout.size.width.toFloat(), text = text)
        }
        labelsThatFit(monthLabels, availableWidth = size.width, gap = labelGapPx).forEach { label ->
            drawText(textMeasurer = textMeasurer, text = label.text, style = labelStyle, topLeft = Offset(label.x, 0f))
        }

        grid.forEachIndexed { weekIndex, week ->
            val x = weekIndex * (cellWidthPx + spacingPx)
            week.forEachIndexed { dayIndex, dayMillis ->
                if (dayMillis == null) return@forEachIndexed
                val y = topPx + dayIndex * (cellHeightPx + spacingPx)
                val value = data[dayMillis] ?: 0f
                drawRoundRect(
                    color = lerp(baseColor, highlightColor, value.coerceIn(0f, 1f)),
                    topLeft = Offset(x, y),
                    size = Size(cellWidthPx, cellHeightPx),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

private fun startOfDayMillis(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** [weeks] columns of 7 Monday..Sunday days each, ending on today's week. Future days are null. */
private fun buildWeekGrid(todayMillis: Long, weeks: Int): List<List<Long?>> {
    val cal = Calendar.getInstance().apply { timeInMillis = todayMillis }
    val isoDayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // Monday=1 .. Sunday=7
    cal.add(Calendar.DAY_OF_YEAR, -(isoDayOfWeek - 1))
    cal.add(Calendar.DAY_OF_YEAR, -(weeks - 1) * 7)

    return List(weeks) {
        List(7) {
            val dayMillis = cal.timeInMillis
            val value = if (dayMillis <= todayMillis) dayMillis else null
            cal.add(Calendar.DAY_OF_YEAR, 1)
            value
        }
    }
}

private fun monthOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MONTH)

/** (column, its Monday) for the first column and each column where a new month begins. */
internal fun monthStartColumns(grid: List<List<Long?>>): List<Pair<Int, Long>> {
    var lastMonth = -1
    return grid.mapIndexedNotNull { weekIndex, week ->
        val monday = week.first() ?: return@mapIndexedNotNull null
        monthOf(monday).takeIf { it != lastMonth }?.let {
            lastMonth = it
            weekIndex to monday
        }
    }
}

internal data class MonthLabel(val x: Float, val width: Float, val text: String)

/**
 * The month labels that fit whole: one that would run into the previous label or past the
 * right edge is left out, instead of overlapping or wrapping into a column of letters.
 */
internal fun labelsThatFit(labels: List<MonthLabel>, availableWidth: Float, gap: Float): List<MonthLabel> {
    var nextFreeX = 0f
    return labels.filter { label ->
        val fits = label.x >= nextFreeX && label.x + label.width <= availableWidth
        if (fits) nextFreeX = label.x + label.width + gap
        fits
    }
}
