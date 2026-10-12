package com.example.heavyduty.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatOneDecimal

/**
 * A premium animated bar chart. Bars grow from 0 to their value on first composition.
 */
@Composable
fun HdBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barMaxHeight: Dp = 120.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFormatter: (Float) -> String = { value -> "${value.formatOneDecimal()} h" }
) {
    if (data.isEmpty()) {
        NoChartData(height = barMaxHeight, color = labelColor, modifier = modifier)
        return
    }
    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)
    val colors = BarColors(bar = barColor, track = trackColor, label = labelColor, value = valueColor)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        data.forEach { (label, value) ->
            HdBar(
                label = label,
                valueText = valueFormatter(value),
                fraction = (value / maxValue).coerceIn(0f, 1f),
                maxHeight = barMaxHeight,
                colors = colors
            )
        }
    }
}

private data class BarColors(val bar: Color, val track: Color, val label: Color, val value: Color)

/** One bar: its value on top, the bar growing to [fraction] of [maxHeight], and its label below. */
@Composable
private fun HdBar(label: String, valueText: String, fraction: Float, maxHeight: Dp, colors: BarColors) {
    val animatedFraction = remember(label) { Animatable(0f) }
    LaunchedEffect(fraction) {
        animatedFraction.animateTo(fraction, animationSpec = tween(durationMillis = 700))
    }
    // Read as one item, "Lu: 1.5 h", rather than the value then its label separately.
    val description = "$label: $valueText"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Text(text = valueText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.value)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(maxHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.track),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFraction.value)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush = Brush.verticalGradient(colors = listOf(colors.bar, colors.bar.copy(alpha = 0.6f))))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.label)
    }
}

/** "No data yet", in the space the chart would take. */
@Composable
internal fun NoChartData(height: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(R.string.state_no_data_yet), style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
