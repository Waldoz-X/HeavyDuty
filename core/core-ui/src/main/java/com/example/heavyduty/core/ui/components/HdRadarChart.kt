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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (data.isEmpty() || data.size < 3) {
        Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text("No hay suficientes datos", color = textColor)
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

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(16.dp)
    ) {
        val radius = (size.minDimension / 2) * 0.75f // leave 25% for text labels
        val center = Offset(size.width / 2, size.height / 2)
        val anglePerItem = 2 * PI / labels.size

        // Draw the web (grid lines)
        val levels = 5
        for (i in 1..levels) {
            val r = radius * (i.toFloat() / levels)
            val path = Path()
            labels.indices.forEach { index ->
                val angle = (index * anglePerItem) - (PI / 2) // Start from top
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = gridColor,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw axes and labels
        labels.indices.forEach { index ->
            val angle = (index * anglePerItem) - (PI / 2)
            
            // Axis line
            val endX = center.x + radius * cos(angle).toFloat()
            val endY = center.y + radius * sin(angle).toFloat()
            drawLine(
                color = gridColor,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )

            // Label text
            val textLayoutResult = textMeasurer.measure(labels[index], style = textStyle)
            val labelRadius = radius * 1.25f // push label outside
            val labelX = center.x + labelRadius * cos(angle).toFloat() - textLayoutResult.size.width / 2
            val labelY = center.y + labelRadius * sin(angle).toFloat() - textLayoutResult.size.height / 2
            
            drawText(
                textMeasurer = textMeasurer,
                text = labels[index],
                style = textStyle,
                topLeft = Offset(labelX, labelY)
            )
        }

        // Draw the data polygon
        val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val dataPath = Path()
        
        labels.indices.forEach { index ->
            val value = values[index] * animationProgress.value
            val normalizedValue = (value / maxValue)
            val r = radius * normalizedValue
            val angle = (index * anglePerItem) - (PI / 2)
            
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            
            if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            
            // Draw points
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
        dataPath.close()

        drawPath(
            path = dataPath,
            color = fillColor,
            style = Fill
        )
        drawPath(
            path = dataPath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
