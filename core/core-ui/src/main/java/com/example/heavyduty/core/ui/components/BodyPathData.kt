package com.example.heavyduty.core.ui.components

import android.content.Context
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import org.json.JSONObject

/**
 * One body view (front or back): its SVG viewBox and the parsed muscle outlines inside it.
 *
 * Geometry sourced from `assets/body_paths.json` — see NOTICE.md for attribution
 * (MuscleMap by Melih Colpan, MIT License, via openGym's JSON conversion).
 */
internal data class BodyView(
    val viewBoxMinX: Float,
    val viewBoxMinY: Float,
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val inertPaths: List<Path>,
    val musclePaths: Map<String, List<Path>>,
)

// Parts that are drawn as silhouette only — they carry no training load.
private val INERT_KEYS = setOf("head", "hair", "neck", "hands", "feet", "knees", "ankles")

// MuscleMap's 18 muscle keys collapsed onto HeavyDuty's MuscleGroup display names.
// A few keys HeavyDuty doesn't model separately (serratus, adductors, hip-flexors,
// tibialis) fold onto their nearest neighbor rather than being dropped or invented.
private val MUSCLE_LABEL: Map<String, String> = mapOf(
    "chest" to "Pecho",
    "abs" to "Abdominales",
    "biceps" to "Bíceps",
    "triceps" to "Tríceps",
    "deltoids" to "Hombros",
    "obliques" to "Oblicuos",
    "quadriceps" to "Cuádriceps",
    "calves" to "Pantorrillas",
    "adductors" to "Cuádriceps",
    "trapezius" to "Trapecios",
    "forearm" to "Antebrazos",
    "tibialis" to "Pantorrillas",
    "serratus" to "Pecho",
    "hip-flexors" to "Cuádriceps",
    "upper-back" to "Dorsales",
    "lower-back" to "Espalda",
    "gluteal" to "Glúteos",
    "hamstring" to "Isquiotibiales",
)

/** Full set of muscle group display-name labels [HdBodyDiagram] can shade. */
val BODY_REGION_LABELS: Set<String> = MUSCLE_LABEL.values.toSet()

internal fun loadBodyPaths(context: Context): Pair<BodyView, BodyView> {
    val json = context.assets.open("body_paths.json").bufferedReader().use { it.readText() }
    val root = JSONObject(json)
    return parseView(root.getJSONObject("front")) to parseView(root.getJSONObject("back"))
}

private fun parseView(viewJson: JSONObject): BodyView {
    val (minX, minY, width, height) = viewJson.getString("vb").split(" ").map { it.toFloat() }
    val pathsJson = viewJson.getJSONObject("p")
    val parser = PathParser()

    val inert = mutableListOf<Path>()
    val muscles = mutableMapOf<String, MutableList<Path>>()

    for (key in pathsJson.keys()) {
        val dArray = pathsJson.getJSONArray(key)
        val paths = (0 until dArray.length()).map { i ->
            parser.parsePathString(dArray.getString(i)).toPath()
        }
        if (key in INERT_KEYS) {
            inert.addAll(paths)
        } else {
            val label = MUSCLE_LABEL[key] ?: continue
            muscles.getOrPut(label) { mutableListOf() }.addAll(paths)
        }
    }

    return BodyView(minX, minY, width, height, inert, muscles)
}
