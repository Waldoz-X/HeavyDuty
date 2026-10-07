package com.example.heavyduty.core.ui.components

import com.example.heavyduty.core.common.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    valueLabelFormatter: (Float) -> String = { value -> "%.0f".format(value) },
    xAxisLabelFormatter: (Long) -> String = { millis ->
        SimpleDateFormat("MMM", Locale.Builder().setLanguage("es").setRegion("ES").build())
            .format(Date(millis))
            .replaceFirstChar { it.uppercase() }
    },
) {
    if (data.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.state_no_data_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
            )
        }
        return
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
            .height(height),
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
        listOf(maxY - padding, (maxY + minY) / 2f, minY + padding).forEach { value ->
            drawText(
                textMeasurer = textMeasurer,
                text = valueLabelFormatter(value),
                style = labelStyle,
                topLeft = Offset(0f, mappedY(value) - 6.sp.toPx()),
            )
        }

        // X-axis labels: first, middle, last point.
        listOf(data.first(), data[data.size / 2], data.last()).forEach { (timestamp, _) ->
            val text = xAxisLabelFormatter(timestamp)
            val layout = textMeasurer.measure(text, style = labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(mappedX(timestamp) - layout.size.width / 2f, chartBottom + 4.dp.toPx()),
            )
        }

        // Dashed goal line.
        if (goalValue != null) {
            val goalY = mappedY(goalValue)
            drawLine(
                color = goalLineColor,
                start = Offset(chartLeft, goalY),
                end = Offset(chartRight, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            val goalText = valueLabelFormatter(goalValue)
            val goalLayout = textMeasurer.measure(goalText, style = goalStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = goalText,
                style = goalStyle,
                topLeft = Offset(chartRight - goalLayout.size.width, goalY - goalLayout.size.height - 2.dp.toPx()),
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
                endY = chartBottom,
            ),
            style = Fill,
        )
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = points.last())
    }
}
