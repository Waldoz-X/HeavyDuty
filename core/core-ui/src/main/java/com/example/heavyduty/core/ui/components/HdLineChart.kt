package com.example.heavyduty.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatOneDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * An animated line + area chart with an optional dashed horizontal goal line — used for
 * trends like body weight over time. Pure Canvas, no charting library, matching the style
 * of [HdBarChart] / [HdRadarChart].
 */
@Composable
fun HdLineChart(
    data: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
    goalValue: Float? = null,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    goalLineColor: Color = MaterialTheme.colorScheme.tertiary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    height: Dp = 160.dp,
    // Null picks a format that suits the data: as many decimals as the labels need to differ,
    // and dates in the app's language at a granularity that fits the span.
    valueLabelFormatter: ((Float) -> String)? = null,
    xAxisLabelFormatter: ((Long) -> String)? = null
) {
    if (data.size < 2) {
        NoChartData(height = height, color = labelColor, modifier = modifier)
        return
    }

    val locale = LocalConfiguration.current.locales[0]
    val formatX = remember(data, locale, xAxisLabelFormatter) {
        dateFormatter(xAxisLabelFormatter, data.last().first - data.first().first, locale)
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp)
    val goalStyle = TextStyle(color = goalLineColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    val description = lineChartDescription(data, formatX, valueLabelFormatter)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .fillMaxWidth()
            .height(height)
    ) {
        val area = lineChartArea(data, goalValue)
        // Y-axis value labels: max, mid, min.
        val yLabels = yAxisLabels(low = area.minY + area.padding, high = area.maxY - area.padding, formatter = valueLabelFormatter)
        drawAxisLabels(area, yLabels, xAxisLabels(data.map { it.first }, formatX), textMeasurer, labelStyle)
        // Dashed goal line.
        goalValue?.let { goal ->
            drawGoalLine(
                y = area.mappedY(goal),
                left = area.left,
                right = area.right,
                label = valueFormatter(valueLabelFormatter, yLabels.map { it.first })(goal),
                color = goalLineColor,
                textMeasurer = textMeasurer,
                style = goalStyle
            )
        }
        drawSeries(data, area, animationProgress.value, lineColor, fillColor)
    }
}

/** "N points from … to …, lowest …, highest …, latest …" for TalkBack. */
@Composable
private fun lineChartDescription(
    data: List<Pair<Long, Float>>,
    formatX: (Long) -> String,
    valueLabelFormatter: ((Float) -> String)?
): String {
    val readValue = valueFormatter(valueLabelFormatter, data.map { it.second })
    return stringResource(
        R.string.a11y_line_chart,
        data.size,
        formatX(data.first().first),
        formatX(data.last().first),
        readValue(data.minOf { it.second }),
        readValue(data.maxOf { it.second }),
        readValue(data.last().second)
    )
}

/** Where the plot sits inside the canvas, and how data maps onto it. */
private class LineChartArea(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val minX: Long,
    val xRange: Float,
    val minY: Float,
    val maxY: Float,
    val padding: Float
) {
    private val yRange = (maxY - minY).coerceAtLeast(0.01f)
    fun mappedX(timestamp: Long) = left + (timestamp - minX) / xRange * (right - left)
    fun mappedY(value: Float) = bottom - (value - minY) / yRange * (bottom - top)
}

/** Room for the labels on the left and bottom; values padded 15% so the line doesn't touch the edges. */
private fun DrawScope.lineChartArea(data: List<Pair<Long, Float>>, goalValue: Float?): LineChartArea {
    val valueRange = data.map { it.second } + listOfNotNull(goalValue)
    val padding = ((valueRange.maxOrNull() ?: 0f) - (valueRange.minOrNull() ?: 0f)) * 0.15f
    return LineChartArea(
        left = 34.dp.toPx(),
        right = size.width,
        top = 4.dp.toPx(),
        bottom = size.height - 18.dp.toPx(),
        minX = data.first().first,
        xRange = (data.last().first - data.first().first).coerceAtLeast(1L).toFloat(),
        minY = (valueRange.minOrNull() ?: 0f) - padding,
        maxY = (valueRange.maxOrNull() ?: 1f) + padding,
        padding = padding
    )
}

private fun DrawScope.drawAxisLabels(
    area: LineChartArea,
    yLabels: List<Pair<Float, String>>,
    xLabels: List<Pair<Long, String>>,
    textMeasurer: TextMeasurer,
    style: TextStyle
) {
    yLabels.forEach { (value, text) ->
        drawText(textMeasurer = textMeasurer, text = text, style = style, topLeft = Offset(0f, area.mappedY(value) - 6.sp.toPx()))
    }
    // First, middle and last point — kept inside the chart so the edge ones aren't cut in half.
    xLabels.forEach { (timestamp, text) ->
        val width = textMeasurer.measure(text, style = style).size.width.toFloat()
        val left = (area.mappedX(timestamp) - width / 2f).coerceIn(area.left, (area.right - width).coerceAtLeast(area.left))
        drawText(textMeasurer = textMeasurer, text = text, style = style, topLeft = Offset(left, area.bottom + 4.dp.toPx()))
    }
}

/** The line and the area under it, growing up from the baseline as [progress] goes 0 → 1. */
private fun DrawScope.drawSeries(data: List<Pair<Long, Float>>, area: LineChartArea, progress: Float, lineColor: Color, fillColor: Color) {
    val points = data.map { (timestamp, value) ->
        val targetY = area.mappedY(value)
        Offset(area.mappedX(timestamp), area.bottom + (targetY - area.bottom) * progress)
    }
    val linePath = Path().apply {
        points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
    }
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(points.last().x, area.bottom)
        lineTo(points.first().x, area.bottom)
        close()
    }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(fillColor.copy(alpha = 0.3f), fillColor.copy(alpha = 0f)),
            startY = area.top,
            endY = area.bottom
        ),
        style = Fill
    )
    drawPath(path = linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))
    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = points.last())
}

/** Dash then gap, in px, of the goal line. */
private val GOAL_DASH_PATTERN = floatArrayOf(10f, 8f)

/** Up to about four months, a label per day ("3 oct"); longer spans only need the month. */
private val DAY_LABEL_MAX_SPAN_MILLIS = TimeUnit.DAYS.toMillis(120)

/**
 * The Y-axis labels to draw, top to bottom: highest, middle, lowest — each text only once.
 * A narrow range (79.6–80.4 kg) gets a decimal instead of reading "80, 80, 80"; a flat
 * line gets a single label.
 */
internal fun yAxisLabels(low: Float, high: Float, formatter: ((Float) -> String)?): List<Pair<Float, String>> {
    val values = listOf(high, (high + low) / 2f, low)
    val format = valueFormatter(formatter, values)
    return values.map { it to format(it) }.distinctBy { it.second }
}

private fun valueFormatter(custom: ((Float) -> String)?, values: List<Float>) = custom ?: autoValueFormatter(values)

private fun dateFormatter(custom: ((Long) -> String)?, spanMillis: Long, locale: Locale) = custom ?: dateLabelFormatter(spanMillis, locale)

private fun DrawScope.drawGoalLine(
    y: Float,
    left: Float,
    right: Float,
    label: String,
    color: Color,
    textMeasurer: TextMeasurer,
    style: TextStyle
) {
    drawLine(
        color = color,
        start = Offset(left, y),
        end = Offset(right, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(GOAL_DASH_PATTERN)
    )
    val layout = textMeasurer.measure(label, style = style)
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        style = style,
        topLeft = Offset(right - layout.size.width, y - layout.size.height - 2.dp.toPx())
    )
}

/**
 * Whole numbers when they already tell [values] apart, one decimal otherwise — always with a
 * point, like every other weight in the app ([formatOneDecimal]).
 */
internal fun autoValueFormatter(values: List<Float>): (Float) -> String {
    val wholeNumbersDiffer = values.map { it.roundToInt() }.distinct().size == values.distinct().size
    return if (wholeNumbersDiffer) { value -> value.roundToInt().toString() } else { value -> value.formatOneDecimal() }
}

/** The X-axis labels to draw: first, middle and last point, skipping repeats ("Oct, Oct, Oct"). */
internal fun xAxisLabels(timestamps: List<Long>, format: (Long) -> String): List<Pair<Long, String>> =
    listOf(timestamps.first(), timestamps[timestamps.size / 2], timestamps.last())
        .distinct()
        .map { it to format(it) }
        .distinctBy { it.second }

internal fun dateLabelFormatter(spanMillis: Long, locale: Locale): (Long) -> String {
    val dateFormat = SimpleDateFormat(if (spanMillis <= DAY_LABEL_MAX_SPAN_MILLIS) "d MMM" else "MMM yy", locale)
    return { millis -> dateFormat.format(Date(millis)).replaceFirstChar { it.titlecase(locale) } }
}
