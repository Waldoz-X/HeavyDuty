package com.example.heavyduty.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.ui.theme.raised

/**
 * A subtle animated gradient sweep, own implementation rather than a third-party dependency
 * (matches the rest of the app's "no extra library for something this small" convention).
 * Sits on the [MaterialTheme.colorScheme.raised] tone (one step up from a card, since a
 * skeleton bar represents content nested inside a card) with a brighter [surfaceBright] band
 * sweeping across it.
 */
@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    // Kept as a State and read only inside drawBehind: every frame repaints the bar without
    // recomposing it (reading it here, during composition, recomposed each bar every frame).
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val base = MaterialTheme.colorScheme.raised
    val highlight = MaterialTheme.colorScheme.surfaceBright
    return this.drawBehind {
        // The band is sized to the element, so short and long bars sweep at the same pace.
        val band = size.width.coerceAtLeast(size.height) * 0.6f
        val x = -band + (size.width + 2 * band) * progress.value
        drawRect(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(x - band, 0f),
                end = Offset(x, 0f)
            )
        )
    }
}

/** One shimmering placeholder bar, standing in for a line of text while content loads. */
@Composable
fun HdSkeletonLine(modifier: Modifier = Modifier, height: Dp = 16.dp) {
    Box(
        modifier = modifier
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .shimmer()
    )
}

/**
 * A card-shaped skeleton (title-width bar + subtitle-width bar) matching the two-line shape
 * most list items in this app use (name + a short metadata line) — swap in for [HdCard] while
 * that real content is loading, so the list's structure stays visible instead of collapsing to
 * a single centered spinner.
 */
@Composable
fun HdSkeletonCard(modifier: Modifier = Modifier) {
    HdCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HdSkeletonLine(modifier = Modifier.fillMaxWidth(0.55f), height = 18.dp)
            HdSkeletonLine(modifier = Modifier.fillMaxWidth(0.3f), height = 12.dp)
        }
    }
}
