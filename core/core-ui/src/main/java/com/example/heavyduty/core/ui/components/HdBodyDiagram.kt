package com.example.heavyduty.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.MuscleGroup

/**
 * A front + back humanoid silhouette used to visualize how much volume was trained per
 * muscle group. Geometry is real anatomical outlines (see [loadBodyPaths] / NOTICE.md),
 * not hand-drawn shapes — this component only shades and fits them.
 *
 * [muscleValues] is keyed by muscle group. Groups without a body region (full body, cardio)
 * are simply not drawn.
 *
 * By default each muscle is shaded on a [baseColor]→[highlightColor] gradient scaled by
 * its value in [muscleValues] (e.g. volume). Pass [categoryColors] to shade by discrete
 * categories instead (e.g. a fatigue traffic light) — when a muscle has an entry there,
 * that exact color is used and its [muscleValues] entry is ignored.
 */
@Composable
fun HdBodyDiagram(
    muscleValues: Map<MuscleGroup, Float>,
    modifier: Modifier = Modifier,
    gender: BodyGender = BodyGender.MALE,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    categoryColors: Map<MuscleGroup, Color>? = null,
    frontLabel: String = stringResource(R.string.body_front),
    backLabel: String = stringResource(R.string.body_back)
) {
    val context = LocalContext.current
    val (front, back) = remember(gender) { loadBodyPaths(context, gender) }

    val maxValue = (front.musclePaths.keys + back.musclePaths.keys)
        .map { muscleValues[it] ?: 0f }
        .maxOrNull()
        ?.coerceAtLeast(1f)
        ?: 1f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                drawBodyView(front, muscleValues, maxValue, baseColor, highlightColor, categoryColors)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = frontLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                drawBodyView(back, muscleValues, maxValue, baseColor, highlightColor, categoryColors)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = backLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawBodyView(
    view: BodyView,
    muscleValues: Map<MuscleGroup, Float>,
    maxValue: Float,
    baseColor: Color,
    highlightColor: Color,
    categoryColors: Map<MuscleGroup, Color>?
) {
    // Fit the SVG viewBox into the available canvas, preserving aspect ratio and centered.
    val fitScale = minOf(size.width / view.viewBoxWidth, size.height / view.viewBoxHeight)
    val drawnWidth = view.viewBoxWidth * fitScale
    val drawnHeight = view.viewBoxHeight * fitScale
    val offsetX = (size.width - drawnWidth) / 2f
    val offsetY = (size.height - drawnHeight) / 2f

    translate(left = offsetX, top = offsetY) {
        scale(scale = fitScale, pivot = Offset.Zero) {
            translate(left = -view.viewBoxMinX, top = -view.viewBoxMinY) {
                view.inertPaths.forEach { path -> drawPath(path, color = baseColor) }
                view.musclePaths.forEach { (muscle, paths) ->
                    val color = categoryColors?.get(muscle) ?: run {
                        val value = muscleValues[muscle] ?: 0f
                        val intensity = (value / maxValue).coerceIn(0f, 1f)
                        lerp(baseColor, highlightColor, intensity)
                    }
                    paths.forEach { path -> drawPath(path, color = color) }
                }
            }
        }
    }
}
