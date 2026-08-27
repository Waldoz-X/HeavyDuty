package com.example.heavyduty.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                drawBodyView(FRONT_REGIONS, muscleValues, maxValue, baseColor, highlightColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = frontLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                drawBodyView(BACK_REGIONS, muscleValues, maxValue, baseColor, highlightColor)
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
) {
    val w = size.width
    val h = size.height

    // Continuous neutral silhouette (head, neck, torso, hips, arms, legs) so the
    // figure always reads as one connected body, even where no muscle is highlighted.
    BASE_SHAPES.forEach { shape -> shape.draw(this, w, h, baseColor) }

    val headRadius = w * 0.11f
    val headCenter = Offset(w * 0.5f, h * 0.075f)
    drawCircle(color = baseColor, radius = headRadius, center = headCenter)

    // Muscle overlays, drawn on top of the base silhouette in intensity-based color.
    regions.forEach { region ->
        val value = muscleValues[region.label] ?: 0f
        val intensity = (value / maxValue).coerceIn(0f, 1f)
        val color = lerp(baseColor, highlightColor, intensity)
        region.spec.draw(this, w, h, color)
    }
}

private data class RectSpec(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val cornerFraction: Float = 0.35f,
) {
    fun draw(scope: DrawScope, w: Float, h: Float, color: Color) {
        scope.drawRoundRect(
            color = color,
            topLeft = Offset(w * left, h * top),
            size = Size(w * width, h * height),
            cornerRadius = CornerRadius(w * width * cornerFraction),
        )
    }
}

private data class BodyRegionSpec(val label: String, val spec: RectSpec)

private fun region(label: String, left: Float, top: Float, width: Float, height: Float, cornerFraction: Float = 0.35f) =
    BodyRegionSpec(label, RectSpec(left, top, width, height, cornerFraction))

/** Full set of muscle group display-name labels this component knows how to place on the body. */
val BODY_REGION_LABELS: Set<String> = setOf(
    "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Antebrazos",
    "Cuádriceps", "Isquiotibiales", "Glúteos", "Pantorrillas", "Abdominales",
    "Oblicuos", "Trapecios", "Dorsales",
)

// Neutral body shapes shared by both views: neck, torso, hips, arms, legs.
// (Head is drawn separately as a circle.)
private val BASE_SHAPES = listOf(
    RectSpec(left = 0.45f, top = 0.12f, width = 0.10f, height = 0.06f, cornerFraction = 0.2f), // neck
    RectSpec(left = 0.30f, top = 0.16f, width = 0.40f, height = 0.30f, cornerFraction = 0.25f), // torso
    RectSpec(left = 0.32f, top = 0.44f, width = 0.36f, height = 0.10f, cornerFraction = 0.4f), // hips
    RectSpec(left = 0.35f, top = 0.50f, width = 0.12f, height = 0.44f, cornerFraction = 0.4f), // left leg
    RectSpec(left = 0.53f, top = 0.50f, width = 0.12f, height = 0.44f, cornerFraction = 0.4f), // right leg
    RectSpec(left = 0.17f, top = 0.18f, width = 0.12f, height = 0.40f, cornerFraction = 0.4f), // left arm
    RectSpec(left = 0.71f, top = 0.18f, width = 0.12f, height = 0.40f, cornerFraction = 0.4f), // right arm
)

private val FRONT_REGIONS = listOf(
    region("Hombros", left = 0.26f, top = 0.17f, width = 0.10f, height = 0.08f),
    region("Hombros", left = 0.64f, top = 0.17f, width = 0.10f, height = 0.08f),
    region("Pecho", left = 0.34f, top = 0.20f, width = 0.32f, height = 0.13f),
    region("Bíceps", left = 0.18f, top = 0.20f, width = 0.10f, height = 0.18f),
    region("Bíceps", left = 0.72f, top = 0.20f, width = 0.10f, height = 0.18f),
    region("Antebrazos", left = 0.18f, top = 0.40f, width = 0.10f, height = 0.17f),
    region("Antebrazos", left = 0.72f, top = 0.40f, width = 0.10f, height = 0.17f),
    region("Abdominales", left = 0.38f, top = 0.33f, width = 0.24f, height = 0.13f),
    region("Oblicuos", left = 0.31f, top = 0.34f, width = 0.06f, height = 0.12f),
    region("Oblicuos", left = 0.63f, top = 0.34f, width = 0.06f, height = 0.12f),
    region("Cuádriceps", left = 0.35f, top = 0.52f, width = 0.12f, height = 0.26f),
    region("Cuádriceps", left = 0.53f, top = 0.52f, width = 0.12f, height = 0.26f),
)

private val BACK_REGIONS = listOf(
    region("Trapecios", left = 0.38f, top = 0.16f, width = 0.24f, height = 0.07f),
    region("Hombros", left = 0.26f, top = 0.17f, width = 0.10f, height = 0.08f),
    region("Hombros", left = 0.64f, top = 0.17f, width = 0.10f, height = 0.08f),
    region("Dorsales", left = 0.32f, top = 0.23f, width = 0.11f, height = 0.17f),
    region("Dorsales", left = 0.57f, top = 0.23f, width = 0.11f, height = 0.17f),
    region("Espalda", left = 0.40f, top = 0.26f, width = 0.20f, height = 0.18f),
    region("Tríceps", left = 0.18f, top = 0.20f, width = 0.10f, height = 0.18f),
    region("Tríceps", left = 0.72f, top = 0.20f, width = 0.10f, height = 0.18f),
    region("Glúteos", left = 0.33f, top = 0.47f, width = 0.34f, height = 0.09f),
    region("Isquiotibiales", left = 0.35f, top = 0.52f, width = 0.12f, height = 0.24f),
    region("Isquiotibiales", left = 0.53f, top = 0.52f, width = 0.12f, height = 0.24f),
    region("Pantorrillas", left = 0.36f, top = 0.78f, width = 0.10f, height = 0.15f),
    region("Pantorrillas", left = 0.54f, top = 0.78f, width = 0.10f, height = 0.15f),
)
