package com.example.heavyduty.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.ui.theme.card

/**
 * Premium card component with subtle gradient border effect.
 *
 * [onClick], when given, is applied *after* the clip so the ripple is bounded by the card's
 * rounded corners — a caller chaining its own `.clickable()` onto [modifier] instead would put
 * it before HdCard's internal clip, letting the ripple spill past the corners as a rectangle.
 */
@Composable
fun HdCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(containerColor)
            .padding(16.dp),
    ) {
        Column(content = content)
    }
}

/**
 * Gradient card variant for highlighted content.
 */
@Composable
fun HdGradientCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(MaterialTheme.shapes.medium)
            .background(
                brush = Brush.linearGradient(colors = gradientColors),
                alpha = 0.15f,
            )
            .background(MaterialTheme.colorScheme.card.copy(alpha = 0.85f))
            .padding(16.dp),
    ) {
        Column(content = content)
    }
}
