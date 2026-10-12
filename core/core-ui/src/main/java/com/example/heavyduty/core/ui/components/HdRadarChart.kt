package com.example.heavyduty.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heavyduty.core.common.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A beautiful Radar Chart (Spider Web chart) used to display multivariate data like muscle distribution.
 */
@Composable
fun HdRadarChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    secondaryData: Map<String, Float>? = null,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    lineColor: Color = MaterialTheme.colorScheme.primary,
    secondaryFillColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
    secondaryLineColor: Color = MaterialTheme.colorScheme.secondary,
    gridColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (data.isEmpty() || data.size < 3) {
        Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text(stringResource(R.string.state_not_enough_data), color = textColor)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        color = textColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    // Ensure we have at least 3 points to draw a polygon
    val labels = data.keys.toList()
    val values = data.values.toList()

    // Animate progress from 0 to 1
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val description = stringResource(
        R.string.a11y_radar_chart,
        shareOfTotal(data).joinToString(", ") { (label, percent) -> "$label $percent%" }
    )
    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(16.dp)
    ) {
        drawRadar(
            labels = labels,
            values = values,
            secondaryValues = secondaryData?.let { labels.map { label -> it[label] ?: 0f } },
            progress = animationProgress.value,
            colors = RadarColors(
                fill = fillColor,
                line = lineColor,
                secondaryFill = secondaryFillColor,
                secondaryLine = secondaryLineColor,
                grid = gridColor
            ),
            textMeasurer = textMeasurer,
            textStyle = textStyle
        )
    }
}

private fun DrawScope.drawRadarPolygon(
    values: List<Float>,
    animationProgress: Float,
    maxValue: Float,
    radius: Float,
    center: Offset,
    anglePerItem: Double,
    fillColor: Color,
    lineColor: Color
) {
    val dataPath = Path()
    values.indices.forEach { index ->
        val value = values[index] * animationProgress
        val normalizedValue = value / maxValue
        val r = radius * normalizedValue
        val angle = (index * anglePerItem) - (PI / 2)

        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()

        if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)

        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = Offset(x, y)
        )
    }
    dataPath.close()

    drawPath(path = dataPath, color = fillColor, style = Fill)
    drawPath(path = dataPath, color = lineColor, style = Stroke(width = 2.dp.toPx()))
}

/** The web: [RADAR_LEVELS] concentric polygons, the first corner at the top. */
private fun DrawScope.drawRadarWeb(sides: Int, radius: Float, center: Offset, anglePerItem: Double, color: Color) {
    for (i in 1..RADAR_LEVELS) {
        val r = radius * (i.toFloat() / RADAR_LEVELS)
        val path = Path()
        for (index in 0 until sides) {
            val angle = (index * anglePerItem) - (PI / 2) // Start from top
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = color, style = Stroke(width = 1.dp.toPx()))
    }
}

/** A line from the centre to each corner, and the corner's label just beyond it. */
private fun DrawScope.drawRadarAxes(
    labels: List<String>,
    radius: Float,
    center: Offset,
    anglePerItem: Double,
    color: Color,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
) {
    labels.indices.forEach { index ->
        val angle = (index * anglePerItem) - (PI / 2)
        val end = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
        drawLine(color = color, start = center, end = end, strokeWidth = 1.dp.toPx())

        val textLayoutResult = textMeasurer.measure(labels[index], style = textStyle)
        val labelRadius = radius * 1.25f // push label outside
        val labelX = center.x + labelRadius * cos(angle).toFloat() - textLayoutResult.size.width / 2
        val labelY = center.y + labelRadius * sin(angle).toFloat() - textLayoutResult.size.height / 2
        drawText(textMeasurer = textMeasurer, text = labels[index], style = textStyle, topLeft = Offset(labelX, labelY))
    }
}

private const val RADAR_LEVELS = 5

private class RadarColors(val fill: Color, val line: Color, val secondaryFill: Color, val secondaryLine: Color, val grid: Color)

/** Web, axes and labels, then the data: [secondaryValues] (if any) underneath, [values] on top. */
private fun DrawScope.drawRadar(
    labels: List<String>,
    values: List<Float>,
    secondaryValues: List<Float>?,
    progress: Float,
    colors: RadarColors,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
) {
    val radius = (size.minDimension / 2) * 0.75f // leave 25% for text labels
    val center = Offset(size.width / 2, size.height / 2)
    val anglePerItem = 2 * PI / labels.size

    drawRadarWeb(sides = labels.size, radius = radius, center = center, anglePerItem = anglePerItem, color = colors.grid)
    drawRadarAxes(
        labels = labels,
        radius = radius,
        center = center,
        anglePerItem = anglePerItem,
        color = colors.grid,
        textMeasurer = textMeasurer,
        textStyle = textStyle
    )

    // Draw the data polygon(s) — scale shared across primary and secondary datasets.
    val maxValue = (values + secondaryValues.orEmpty()).maxOrNull()?.coerceAtLeast(1f) ?: 1f
    // The previous period underneath, the current one on top.
    listOfNotNull(
        secondaryValues?.let { Triple(it, colors.secondaryFill, colors.secondaryLine) },
        Triple(values, colors.fill, colors.line)
    ).forEach { (series, fill, line) ->
        drawRadarPolygon(
            values = series,
            animationProgress = progress,
            maxValue = maxValue,
            radius = radius,
            center = center,
            anglePerItem = anglePerItem,
            fillColor = fill,
            lineColor = line
        )
    }
}
