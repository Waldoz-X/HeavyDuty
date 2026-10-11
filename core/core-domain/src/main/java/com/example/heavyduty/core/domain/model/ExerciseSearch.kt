package com.example.heavyduty.core.domain.model

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * In-memory exercise search. The catalog's names are English ("barbell bench press"), but
 * lifters search in Spanish ("press banca con barra"), without accents, in any word order —
 * so the query is normalized, Spanish gym vocabulary is mapped to the catalog's English terms,
 * and every word must match somewhere in the exercise (name, equipment or muscle group).
 */
fun List<Exercise>.searchExercises(query: String): List<Exercise> {
    val terms = parseQuery(query)
    // Alphabetical by the name shown, in the app's language: the database orders by the stored
    // (Spanish) name, which in English read as random, and puts accented letters after "z".
    val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    if (terms.isEmpty()) return sortedWith(compareBy(collator) { it.displayName })
    return mapNotNull { exercise ->
        val haystack = exercise.searchText()
        if (terms.all { alternatives -> alternatives.any { it in haystack } }) exercise to exercise.rank(terms) else null
    }.sortedWith(
        compareBy<Pair<Exercise, Int>>({ it.second }, { it.first.displayName.length })
            .thenBy(collator) { it.first.displayName }
    ).map { it.first }
}

/** Each query word becomes the set of strings any of which counts as a match for it. */
internal fun parseQuery(query: String): List<Set<String>> {
    var text = " ${normalize(query)} "
    // Multi-word Spanish terms first, so "peso muerto" isn't read as "weight" + "dead".
    PHRASES.forEach { (phrase, english) -> text = text.replace(" $phrase ", " $english ") }
    return text.split(' ')
        .filter { it.isNotBlank() && it !in STOPWORDS }
        .map { word -> setOf(word) + SYNONYMS[word].orEmpty() + SYNONYMS[word.singular()].orEmpty() }
}

internal fun normalize(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(DIACRITICS, "")
    .replace(NON_ALPHANUMERIC, " ")
    .replace(WHITESPACE, " ")
    .trim()

private fun Exercise.searchText(): String = normalize("${names()} $equipment ${muscleGroup.name} ${exerciseType.name}")

/** Displayed (Spanish) and original (English) names, plus unhyphenated forms ("pullup"). */
private fun Exercise.names(): String = "$name $nameEn ${name.replace("-", "")} ${nameEn.replace("-", "")}"

/**
 * 0 when every word hits the displayed (Spanish) name, 1 when it takes the English name too,
 * 2 when some words only match equipment/muscle — so "press banca barra" lists "Press de banca
 * con barra" before "Press JM con barra" (which matches "banca" only via "JM bench press").
 */
private fun Exercise.rank(terms: List<Set<String>>): Int {
    fun matchesAll(text: String) = terms.all { alternatives -> alternatives.any { it in text } }
    return when {
        matchesAll(normalize("$name ${name.replace("-", "")}")) -> 0
        matchesAll(normalize(names())) -> 1
        else -> 2
    }
}

private fun String.singular(): String = when {
    endsWith("es") && length - 2 >= MIN_SINGULAR_LENGTH -> dropLast(2)
    endsWith("s") && length - 1 >= MIN_SINGULAR_LENGTH -> dropLast(1)
    else -> this
}

/** Shorter words are left alone: "res" isn't the plural of "re". */
private const val MIN_SINGULAR_LENGTH = 3

private val DIACRITICS = "\\p{Mn}+".toRegex()
private val NON_ALPHANUMERIC = "[^a-z0-9]+".toRegex()
private val WHITESPACE = "\\s+".toRegex()

private val STOPWORDS = setOf("de", "del", "con", "en", "la", "el", "los", "las", "a", "al", "y", "por", "para", "un", "una")

private val PHRASES = listOf(
    "peso muerto" to "deadlift",
    "pesa rusa" to "kettlebell",
    "empuje de cadera" to "hip_thrust",
    "hip thrust" to "hip_thrust",
    "buenos dias" to "good_morning",
    "press militar" to "military_press",
    "jalon al pecho" to "pulldown",
    "face pull" to "face_pull",
    "peso corporal" to "body_weight",
    "maquina smith" to "smith",
    "press frances" to "skull_crusher",
    "de pie" to "standing"
).map { (es, en) -> normalize(es) to en }

private val SYNONYMS: Map<String, Set<String>> = mapOf(
    "deadlift" to setOf("deadlift"),
    "kettlebell" to setOf("kettlebell"),
    "hip_thrust" to setOf("hip thrust", "hipthrust", "glute bridge"),
    "good_morning" to setOf("good morning"),
    "military_press" to setOf("military press", "overhead press", "shoulder press"),
    "pulldown" to setOf("pulldown", "pull down"),
    "face_pull" to setOf("face pull"),
    "body_weight" to setOf("body weight"),
    "skull_crusher" to setOf("skull crusher", "lying triceps extension"),
    "banca" to setOf("bench"),
    "banco" to setOf("bench"),
    "pecho" to setOf("chest", "bench", "fly", "push up", "pushup"),
    "pectoral" to setOf("chest"),
    "inclinado" to setOf("incline"),
    "inclinada" to setOf("incline"),
    "declinado" to setOf("decline"),
    "declinada" to setOf("decline"),
    "sentadilla" to setOf("squat"),
    "muerto" to setOf("deadlift"),
    "dominada" to setOf("pull up", "pullup", "chin up", "chinup"),
    "remo" to setOf("row"),
    "zancada" to setOf("lunge"),
    "estocada" to setOf("lunge"),
    "desplante" to setOf("lunge"),
    "fondo" to setOf("dip"),
    "flexion" to setOf("push up", "pushup"),
    "lagartija" to setOf("push up", "pushup"),
    "curl" to setOf("curl"),
    "bicep" to setOf("biceps", "curl"),
    "tricep" to setOf("triceps", "pushdown", "extension"),
    "extension" to setOf("extension"),
    "hombro" to setOf("shoulder", "delt", "press", "raise"),
    "militar" to setOf("military", "overhead"),
    "elevacion" to setOf("raise"),
    "lateral" to setOf("lateral", "side"),
    "frontal" to setOf("front"),
    "jalon" to setOf("pulldown", "pull down"),
    "polea" to setOf("cable"),
    "cable" to setOf("cable"),
    "mancuerna" to setOf("dumbbell"),
    "barra" to setOf("barbell", "bar"),
    "maquina" to setOf("machine", "lever", "leverage", "sled", "smith"),
    "banda" to setOf("band"),
    "liga" to setOf("band"),
    "abdominal" to setOf("crunch", "sit up", "situp", "abs", "waist"),
    "abdomen" to setOf("crunch", "sit up", "abs", "waist"),
    "plancha" to setOf("plank"),
    "gemelo" to setOf("calf", "calves"),
    "pantorrilla" to setOf("calf", "calves"),
    "cadera" to setOf("hip"),
    "puente" to setOf("bridge"),
    "prensa" to setOf("leg press", "press"),
    "pierna" to setOf("leg", "squat", "lunge"),
    "femoral" to setOf("hamstring", "leg curl"),
    "isquiotibial" to setOf("hamstring"),
    "cuadricep" to setOf("quad", "leg extension", "squat"),
    "gluteo" to setOf("glute", "hip"),
    "espalda" to setOf("back", "row", "pulldown", "pull up"),
    "dorsal" to setOf("lat", "pulldown"),
    "apertura" to setOf("fly", "flye"),
    "cruce" to setOf("crossover"),
    "encogimiento" to setOf("shrug"),
    "trapecio" to setOf("shrug", "trap"),
    "salto" to setOf("jump"),
    "hiperextension" to setOf("hyperextension"),
    "pajaro" to setOf("reverse fly", "rear delt"),
    "antebrazo" to setOf("wrist", "forearm"),
    "muneca" to setOf("wrist"),
    "agarre" to setOf("grip"),
    "cerrado" to setOf("close grip", "narrow"),
    "abierto" to setOf("wide"),
    "inverso" to setOf("reverse"),
    "invertido" to setOf("reverse", "inverted"),
    "sentado" to setOf("seated", "sitting"),
    "standing" to setOf("standing"),
    "parado" to setOf("standing"),
    "acostado" to setOf("lying"),
    "tumbado" to setOf("lying"),
    "unilateral" to setOf("single", "one arm", "one leg"),
    "cardio" to setOf("cardio"),
    "estiramiento" to setOf("stretch")
)
