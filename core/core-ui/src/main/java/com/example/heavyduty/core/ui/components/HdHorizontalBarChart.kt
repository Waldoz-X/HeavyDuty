package com.example.heavyduty.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One row per item — label, bar, value — for lists whose labels are words and whose length
 * isn't fixed (muscle groups). [HdBarChart] suits a handful of short labels, like weekdays;
 * side by side, a dozen muscle names run into each other. Bars grow in on first composition.
 */
@Composable
fun HdHorizontalBarChart(
    data: List<Pair<String, Float>>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
) {
    if (data.isEmpty()) {
        NoChartData(height = 120.dp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        return
    }
    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEach { (label, value) ->
            HorizontalBar(
                label = label,
                valueText = valueFormatter(value),
                fraction = (value / maxValue).coerceIn(0f, 1f),
                barColor = barColor,
                trackColor = trackColor
            )
        }
    }
}

@Composable
private fun HorizontalBar(label: String, valueText: String, fraction: Float, barColor: Color, trackColor: Color) {
    val animatedFraction = remember(label) { Animatable(0f) }
    LaunchedEffect(fraction) {
        animatedFraction.animateTo(fraction, animationSpec = tween(durationMillis = 700))
    }
    // Read as one item, "Pecho: 5.4", rather than the label, the bar and the value separately.
    val description = "$label: $valueText"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(LABEL_WIDTH)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(BAR_HEIGHT / 2))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.value)
                    .clip(RoundedCornerShape(BAR_HEIGHT / 2))
                    .background(brush = Brush.horizontalGradient(colors = listOf(barColor.copy(alpha = 0.6f), barColor)))
            )
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(VALUE_WIDTH)
        )
    }
}

private val LABEL_WIDTH = 112.dp
private val VALUE_WIDTH = 48.dp
private val BAR_HEIGHT = 14.dp
