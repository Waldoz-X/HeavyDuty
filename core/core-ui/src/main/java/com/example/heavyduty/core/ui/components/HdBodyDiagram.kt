package com.example.heavyduty.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * A simplified front + back humanoid silhouette used to visualize how much volume was
 * trained per muscle group. Purely vector (Canvas), no external body art.
 *
 * Keys in [muscleValues] are muscle group display names (e.g. "Pecho", "Espalda",
 * "Hombros" — see [BODY_REGION_LABELS] for the full recognized set). Unrecognized keys
 * (like whole-body or cardio categories) are simply not drawn.
 */
@Composable
fun HdBodyDiagram(
    muscleValues: Map<String, Float>,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    outlineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    frontLabel: String = "Frontal",
    backLabel: String = "Posterior",
) {
    val maxValue = (FRONT_REGIONS + BACK_REGIONS)
        .map { muscleValues[it.label] ?: 0f }
        .maxOrNull()
        ?.coerceAtLeast(1f)
        ?: 1f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.5f),
            ) {
                drawBodyView(FRONT_REGIONS, muscleValues, maxValue, baseColor, highlightColor, outlineColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = frontLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.5f),
            ) {
                drawBodyView(BACK_REGIONS, muscleValues, maxValue, baseColor, highlightColor, outlineColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = backLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun DrawScope.drawBodyView(
    regions: List<BodyRegionSpec>,
    muscleValues: Map<String, Float>,
    maxValue: Float,
    baseColor: Color,
    highlightColor: Color,
    outlineColor: Color,
) {
    val w = size.width
    val h = size.height

    // Neutral silhouette anchors: head, neck, and an outline of the figure.
    val headRadius = w * 0.14f
    val headCenter = Offset(w * 0.5f, h * 0.075f)
    drawCircle(color = baseColor, radius = headRadius, center = headCenter)

    drawRoundRect(
        color = baseColor,
        topLeft = Offset(w * 0.44f, h * 0.13f),
        size = Size(w * 0.12f, h * 0.05f),
        cornerRadius = CornerRadius(w * 0.02f),
    )

    val outline = Path().apply {
        moveTo(w * 0.30f, h * 0.20f)
        lineTo(w * 0.70f, h * 0.20f)
        lineTo(w * 0.82f, h * 0.42f)
        lineTo(w * 0.66f, h * 0.50f)
        lineTo(w * 0.64f, h * 0.60f)
        lineTo(w * 0.66f, h * 0.94f)
        lineTo(w * 0.54f, h * 0.94f)
        lineTo(w * 0.52f, h * 0.62f)
        lineTo(w * 0.48f, h * 0.62f)
        lineTo(w * 0.46f, h * 0.94f)
        lineTo(w * 0.34f, h * 0.94f)
        lineTo(w * 0.36f, h * 0.60f)
        lineTo(w * 0.34f, h * 0.50f)
        lineTo(w * 0.18f, h * 0.42f)
        close()
    }
    drawPath(path = outline, color = outlineColor, style = Stroke(width = 1.dp.toPx()))

    regions.forEach { region ->
        val value = muscleValues[region.label] ?: 0f
        val intensity = (value / maxValue).coerceIn(0f, 1f)
        val color = lerp(baseColor, highlightColor, intensity)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * region.left, h * region.top),
            size = Size(w * region.width, h * region.height),
            cornerRadius = CornerRadius(w * region.width * region.cornerFraction),
        )
    }
}

private data class BodyRegionSpec(
    val label: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val cornerFraction: Float = 0.35f,
)

/** Full set of muscle group display-name labels this component knows how to place on the body. */
val BODY_REGION_LABELS: Set<String> = setOf(
    "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Antebrazos",
    "Cuádriceps", "Isquiotibiales", "Glúteos", "Pantorrillas", "Abdominales",
    "Oblicuos", "Trapecios", "Dorsales",
)

private val FRONT_REGIONS = listOf(
    BodyRegionSpec("Hombros", left = 0.24f, top = 0.19f, width = 0.11f, height = 0.07f),
    BodyRegionSpec("Hombros", left = 0.65f, top = 0.19f, width = 0.11f, height = 0.07f),
    BodyRegionSpec("Pecho", left = 0.32f, top = 0.21f, width = 0.36f, height = 0.13f),
    BodyRegionSpec("Bíceps", left = 0.19f, top = 0.24f, width = 0.10f, height = 0.16f),
    BodyRegionSpec("Bíceps", left = 0.71f, top = 0.24f, width = 0.10f, height = 0.16f),
    BodyRegionSpec("Antebrazos", left = 0.16f, top = 0.40f, width = 0.10f, height = 0.16f),
    BodyRegionSpec("Antebrazos", left = 0.74f, top = 0.40f, width = 0.10f, height = 0.16f),
    BodyRegionSpec("Abdominales", left = 0.38f, top = 0.34f, width = 0.24f, height = 0.15f),
    BodyRegionSpec("Oblicuos", left = 0.32f, top = 0.36f, width = 0.06f, height = 0.13f),
    BodyRegionSpec("Oblicuos", left = 0.62f, top = 0.36f, width = 0.06f, height = 0.13f),
    BodyRegionSpec("Cuádriceps", left = 0.34f, top = 0.58f, width = 0.13f, height = 0.22f),
    BodyRegionSpec("Cuádriceps", left = 0.53f, top = 0.58f, width = 0.13f, height = 0.22f),
)

private val BACK_REGIONS = listOf(
    BodyRegionSpec("Trapecios", left = 0.38f, top = 0.16f, width = 0.24f, height = 0.08f),
    BodyRegionSpec("Hombros", left = 0.24f, top = 0.19f, width = 0.11f, height = 0.07f),
    BodyRegionSpec("Hombros", left = 0.65f, top = 0.19f, width = 0.11f, height = 0.07f),
    BodyRegionSpec("Dorsales", left = 0.29f, top = 0.24f, width = 0.14f, height = 0.18f),
    BodyRegionSpec("Dorsales", left = 0.57f, top = 0.24f, width = 0.14f, height = 0.18f),
    BodyRegionSpec("Espalda", left = 0.40f, top = 0.28f, width = 0.20f, height = 0.20f),
    BodyRegionSpec("Tríceps", left = 0.19f, top = 0.24f, width = 0.10f, height = 0.16f),
    BodyRegionSpec("Tríceps", left = 0.71f, top = 0.24f, width = 0.10f, height = 0.16f),
    BodyRegionSpec("Glúteos", left = 0.35f, top = 0.49f, width = 0.30f, height = 0.10f),
    BodyRegionSpec("Isquiotibiales", left = 0.35f, top = 0.60f, width = 0.13f, height = 0.19f),
    BodyRegionSpec("Isquiotibiales", left = 0.52f, top = 0.60f, width = 0.13f, height = 0.19f),
    BodyRegionSpec("Pantorrillas", left = 0.36f, top = 0.80f, width = 0.11f, height = 0.14f),
    BodyRegionSpec("Pantorrillas", left = 0.53f, top = 0.80f, width = 0.11f, height = 0.14f),
)
