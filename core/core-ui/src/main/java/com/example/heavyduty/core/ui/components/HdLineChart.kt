package com.example.heavyduty.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
        Box(
            modifier = modifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.state_no_data_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
        }
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

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val bottomLabelHeight = 18.dp.toPx()
        val leftLabelWidth = 34.dp.toPx()
        val chartLeft = leftLabelWidth
        val chartRight = size.width
        val chartTop = 4.dp.toPx()
        val chartBottom = size.height - bottomLabelHeight

        val minX = data.first().first
        val maxX = data.last().first
        val xRange = (maxX - minX).coerceAtLeast(1L).toFloat()

        val rawValues = data.map { it.second }
        val valueRange = (rawValues + listOfNotNull(goalValue))
        val padding = ((valueRange.maxOrNull() ?: 0f) - (valueRange.minOrNull() ?: 0f)) * 0.15f
        val minY = (valueRange.minOrNull() ?: 0f) - padding
        val maxY = (valueRange.maxOrNull() ?: 1f) + padding
        val yRange = (maxY - minY).coerceAtLeast(0.01f)

        fun mappedX(timestamp: Long) = chartLeft + (timestamp - minX) / xRange * (chartRight - chartLeft)
        fun mappedY(value: Float) = chartBottom - (value - minY) / yRange * (chartBottom - chartTop)

        // Y-axis value labels: max, mid, min.
        val yLabels = yAxisLabels(low = minY + padding, high = maxY - padding, formatter = valueLabelFormatter)
        yLabels.forEach { (value, text) ->
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(0f, mappedY(value) - 6.sp.toPx())
            )
        }

        // X-axis labels: first, middle, last point — kept inside the chart so the edge ones
        // aren't cut in half.
        xAxisLabels(data.map { it.first }, formatX).forEach { (timestamp, text) ->
            val width = textMeasurer.measure(text, style = labelStyle).size.width.toFloat()
            val left = (mappedX(timestamp) - width / 2f).coerceIn(chartLeft, (chartRight - width).coerceAtLeast(chartLeft))
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(left, chartBottom + 4.dp.toPx())
            )
        }

        // Dashed goal line.
        goalValue?.let { goal ->
            drawGoalLine(
                y = mappedY(goal),
                left = chartLeft,
                right = chartRight,
                label = valueFormatter(valueLabelFormatter, yLabels.map { it.first })(goal),
                color = goalLineColor,
                textMeasurer = textMeasurer,
                style = goalStyle
            )
        }

        // Animated line + area fill, growing up from the baseline.
        val progress = animationProgress.value
        val points = data.map { (timestamp, value) ->
            val targetY = mappedY(value)
            val animatedY = chartBottom + (targetY - chartBottom) * progress
            Offset(mappedX(timestamp), animatedY)
        }

        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, chartBottom)
            lineTo(points.first().x, chartBottom)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor.copy(alpha = 0.3f), fillColor.copy(alpha = 0f)),
                startY = chartTop,
                endY = chartBottom
            ),
            style = Fill
        )
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = points.last())
    }
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
