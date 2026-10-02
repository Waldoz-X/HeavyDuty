package com.example.heavyduty.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Text whose changed characters roll in like an odometer ("01:30" → "01:29" only moves the
 * last digit). Characters are keyed from the right, so "10:00" → "9:59" still lines up.
 *
 * Use it for values that change one step at a time (countdowns, counters); [countingDown]
 * makes new digits come from above, as on a countdown.
 */
@Composable
fun HdRollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontWeight: FontWeight? = null,
    countingDown: Boolean = false,
) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val direction = if (countingDown) -1 else 1
                        (slideInVertically(tween(ROLL_MS)) { direction * it } + fadeIn(tween(ROLL_MS)))
                            .togetherWith(slideOutVertically(tween(ROLL_MS)) { -direction * it } + fadeOut(tween(ROLL_MS)))
                            .using(SizeTransform(clip = true))
                    },
                    label = "rollingChar",
                ) { shown ->
                    Text(text = shown.toString(), style = style, color = color, fontWeight = fontWeight)
                }
            }
        }
    }
}

private const val ROLL_MS = 220
