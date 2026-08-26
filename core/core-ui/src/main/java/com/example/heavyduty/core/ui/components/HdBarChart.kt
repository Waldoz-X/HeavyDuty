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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    valueFormatter: (Float) -> String = { value -> "%.1fh".format(value) },
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(barMaxHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Sin datos todavía",
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
            )
        }
        return
    }

    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        data.forEach { (label, value) ->
            val targetFraction = (value / maxValue).coerceIn(0f, 1f)
            val animatedFraction = remember(label) { Animatable(0f) }
            LaunchedEffect(targetFraction) {
                animatedFraction.animateTo(targetFraction, animationSpec = tween(durationMillis = 700))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = valueColor,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barMaxHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(trackColor),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFraction.value)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(barColor, barColor.copy(alpha = 0.6f)),
                                ),
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
    }
}
