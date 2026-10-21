package com.example.heavyduty.core.ui.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/**
 * The sizes a shareable image comes in, at the resolution the social apps use natively, so they
 * show it as made instead of rescaling it. Content is designed [DESIGN_WIDTH_DP] wide whatever
 * the format: only the height changes.
 */
enum class ShareImageFormat(val widthPx: Int, val heightPx: Int) {
    /** 9:16, full screen: Instagram and WhatsApp stories. */
    STORY(1080, 1920),

    /** 4:5, the tallest a feed post shows uncropped. */
    POST(1080, 1350);

    /** The format's height in the design's dp, for laying content out. */
    val designHeightDp: Float get() = DESIGN_WIDTH_DP * heightPx / widthPx
}

/** Width share content is designed at: 1 dp of it is 3 px of the exported 1080 px image. */
const val DESIGN_WIDTH_DP = 360f

/**
 * Lays [content] out at the full size of [format] (as if on a 1080-px-wide screen, whatever this
 * phone's density or font size), records it into [layer] and shows it scaled down to fit. What
 * is previewed is exactly what [layer] exports — at full resolution, not the preview's.
 */
@Composable
fun ShareImageCanvas(format: ShareImageFormat, layer: GraphicsLayer, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            // As big as fits, whether the space is too narrow or too short for it.
            .aspectRatio(format.widthPx.toFloat() / format.heightPx, matchHeightConstraintsFirst = true)
            .drawWithContent {
                // Draws nothing itself (see below), but has to run for the layer to be recorded.
                drawContent()
                scale(size.width / format.widthPx, pivot = Offset.Zero) { drawLayer(layer) }
            }
    ) {
        Box(
            modifier = Modifier
                // Full export size, taking no room: the preview above is what's on screen.
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints.fixed(format.widthPx, format.heightPx))
                    layout(0, 0) { placeable.place(0, 0) }
                }
                .drawWithContent { layer.record { this@drawWithContent.drawContent() } }
        ) {
            CompositionLocalProvider(LocalDensity provides Density(density = format.widthPx / DESIGN_WIDTH_DP, fontScale = 1f)) {
                content()
            }
        }
    }
}
