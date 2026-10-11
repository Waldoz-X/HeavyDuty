package com.example.heavyduty.core.ui.components

import android.content.Context
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import com.example.heavyduty.core.domain.model.MuscleGroup
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
    val musclePaths: Map<MuscleGroup, List<Path>>
)

// Parts that are drawn as silhouette only — they carry no training load.
private val INERT_KEYS = setOf("head", "hair", "neck", "hands", "feet", "knees", "ankles")

// MuscleMap's 18 muscle keys collapsed onto HeavyDuty's muscle groups. A few keys HeavyDuty
// doesn't model separately (serratus, adductors, hip-flexors, tibialis) fold onto their nearest
// neighbor rather than being dropped or invented. Keyed by the enum, not a display name, so the
// map shades the same muscles in any app language.
private val MUSCLE_GROUP: Map<String, MuscleGroup> = mapOf(
    "chest" to MuscleGroup.CHEST,
    "abs" to MuscleGroup.ABS,
    "biceps" to MuscleGroup.BICEPS,
    "triceps" to MuscleGroup.TRICEPS,
    "deltoids" to MuscleGroup.SHOULDERS,
    "obliques" to MuscleGroup.OBLIQUES,
    "quadriceps" to MuscleGroup.QUADRICEPS,
    "calves" to MuscleGroup.CALVES,
    "adductors" to MuscleGroup.QUADRICEPS,
    "trapezius" to MuscleGroup.TRAPS,
    "forearm" to MuscleGroup.FOREARMS,
    "tibialis" to MuscleGroup.CALVES,
    "serratus" to MuscleGroup.CHEST,
    "hip-flexors" to MuscleGroup.QUADRICEPS,
    "upper-back" to MuscleGroup.LATS,
    "lower-back" to MuscleGroup.BACK,
    "gluteal" to MuscleGroup.GLUTES,
    "hamstring" to MuscleGroup.HAMSTRINGS
)

/** Which body figure [HdBodyDiagram] draws — a user-facing preference, not an assumption. */
enum class BodyGender(val storageValue: String) {
    MALE("male"),
    FEMALE("female")
    ;

    companion object {
        fun fromStorageValue(value: String): BodyGender = entries.find { it.storageValue == value } ?: MALE
    }
}

internal fun loadBodyPaths(context: Context, gender: BodyGender): Pair<BodyView, BodyView> {
    val json = context.assets.open("body_paths.json").bufferedReader().use { it.readText() }
    val root = JSONObject(json).getJSONObject(gender.storageValue)
    return parseView(root.getJSONObject("front")) to parseView(root.getJSONObject("back"))
}

// An SVG viewBox is four numbers: min-x, min-y, width, height.
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun parseView(viewJson: JSONObject): BodyView {
    val (minX, minY, width, height) = viewJson.getString("vb").split(" ").map { it.toFloat() }
    val pathsJson = viewJson.getJSONObject("p")
    val parser = PathParser()

    val inert = mutableListOf<Path>()
    val muscles = mutableMapOf<MuscleGroup, MutableList<Path>>()

    for (key in pathsJson.keys()) {
        val dArray = pathsJson.getJSONArray(key)
        val paths = (0 until dArray.length()).map { i ->
            parser.parsePathString(dArray.getString(i)).toPath()
        }
        if (key in INERT_KEYS) {
            inert.addAll(paths)
        } else {
            val muscle = MUSCLE_GROUP[key] ?: continue
            muscles.getOrPut(muscle) { mutableListOf() }.addAll(paths)
        }
    }

    return BodyView(minX, minY, width, height, inert, muscles)
}
