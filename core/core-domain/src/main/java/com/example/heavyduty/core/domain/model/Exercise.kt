package com.example.heavyduty.core.domain.model

/**
 * Domain model representing an exercise in the catalog.
 */
data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val instructions: String = "",
    val imageUrl: String = "",
    val gifUrl: String = "",
    /** The single "primary" muscle — still what every badge, filter, and accent color uses. */
    val muscleGroup: MuscleGroup,
    val exerciseType: ExerciseType,
    val equipment: String = "",
    val isCustom: Boolean = false,
    /** Unit this exercise's weight is entered/displayed in — storage is always kg regardless, see [WeightUnit]. */
    val weightUnit: WeightUnit = WeightUnit.KG,
    /**
     * Fractional credit per muscle worked, summing to ~1.0 (a compound movement's secondary
     * muscles come from the catalog's real secondary_muscles data, not a guess) — use this,
     * not [muscleGroup] alone, for volume/fatigue/set-count/strength aggregations, so a
     * compound exercise credits every muscle it actually trains instead of only its primary.
     * Defaults to crediting [muscleGroup] alone when no secondary data exists.
     */
    val muscleWeights: Map<MuscleGroup, Float> = mapOf(muscleGroup to 1f),
)

/**
 * Distributes [amount] across this exercise's [Exercise.muscleWeights] instead of crediting
 * only its primary [Exercise.muscleGroup] — the shared, single point every volume/fatigue/
 * set-count/strength calculation should go through, matching the [excludeWarmups] convention.
 */
fun Exercise.muscleCredits(amount: Float): Map<MuscleGroup, Float> =
    muscleWeights.mapValues { (_, weight) -> amount * weight }

/**
 * Inferred from [Exercise.equipment] rather than a stored flag, per the catalog's own
 * "body weight" equipment value — push-ups, pull-ups, etc. shouldn't need a schema change to
 * be recognized. UI built on this should still let the lifter log a weight when they want to
 * (e.g. a weighted vest) — this only means "don't assume/require one".
 */
val Exercise.isBodyweight: Boolean
    get() = equipment.equals("body weight", ignoreCase = true)

/**
 * Spanish label for the catalog's raw `equipment` values (28 distinct strings, all English —
 * the dataset has no localized equipment field). [Exercise.equipment] itself stays untranslated
 * in storage on purpose: [isBodyweight] and the exercise-list equipment filter both match
 * against it directly, so translating the stored value would silently break both. This is a
 * display-only lookup, analogous to [MuscleGroup]'s nameResId but for a free-text field instead
 * of an enum.
 */
private val EQUIPMENT_DISPLAY_NAMES = mapOf(
    "assisted" to "Asistido",
    "band" to "Banda",
    "barbell" to "Barra",
    "body weight" to "Peso corporal",
    "bosu ball" to "Bosu",
    "cable" to "Polea",
    "dumbbell" to "Mancuerna",
    "elliptical machine" to "Elíptica",
    "ez barbell" to "Barra Z",
    "hammer" to "Martillo",
    "kettlebell" to "Pesa rusa",
    "leverage machine" to "Máquina de palanca",
    "medicine ball" to "Balón medicinal",
    "olympic barbell" to "Barra olímpica",
    "resistance band" to "Banda de resistencia",
    "roller" to "Rodillo",
    "rope" to "Cuerda",
    "skierg machine" to "Máquina SkiErg",
    "sled machine" to "Trineo",
    "smith machine" to "Máquina Smith",
    "stability ball" to "Balón de estabilidad",
    "stationary bike" to "Bicicleta estática",
    "stepmill machine" to "Escaladora",
    "tire" to "Llanta",
    "trap bar" to "Barra hexagonal",
    "upper body ergometer" to "Ergómetro de brazos",
    "weighted" to "Con peso",
    "wheel roller" to "Rueda abdominal",
)

/** Translates a raw catalog equipment value (e.g. "body weight") to its Spanish display label. */
fun equipmentLabelFor(rawEquipment: String): String =
    EQUIPMENT_DISPLAY_NAMES[rawEquipment.trim().lowercase()] ?: rawEquipment.ifBlank { "Sin equipo" }

val Exercise.equipmentDisplayName: String
    get() = equipmentLabelFor(equipment)

private val PER_SIDE_NAME_KEYWORDS = listOf(
    "single leg", "single-leg", "single arm", "single-arm",
    "one arm", "one-arm", "one leg", "one-leg",
    "lunge", "alternating", "unilateral", "split squat", "pistol squat", "step-up", "step up",
)

/**
 * Inferred from the exercise name, like [isBodyweight] — no catalog field or exercise-editor
 * UI exists to tag this per-exercise, so a keyword heuristic is the practical option. Reps
 * logged for these exercises are the TOTAL across both sides; UI built on this should show
 * the per-side split rather than assume the raw number IS per side.
 */
val Exercise.isPerSide: Boolean
    get() = PER_SIDE_NAME_KEYWORDS.any { name.contains(it, ignoreCase = true) }
