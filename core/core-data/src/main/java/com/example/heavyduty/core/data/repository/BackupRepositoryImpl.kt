package com.example.heavyduty.core.data.repository

import androidx.room.withTransaction
import com.example.heavyduty.core.data.database.HeavyDutyDatabase
import com.example.heavyduty.core.data.database.dao.BodyWeightDao
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.example.heavyduty.core.data.database.dao.RoutineDao
import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import com.example.heavyduty.core.data.database.entity.ExerciseEntity
import com.example.heavyduty.core.data.database.entity.RoutineEntity
import com.example.heavyduty.core.data.database.entity.RoutineExerciseEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSessionEntity
import com.example.heavyduty.core.data.database.entity.WorkoutSetEntity
import com.example.heavyduty.core.domain.repository.BackupImportResult
import com.example.heavyduty.core.domain.repository.BackupRepository
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.ProgressionPolicy
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.WorkoutType
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_FORMAT_VERSION = 1
private const val STATUS_IN_PROGRESS = "IN_PROGRESS"
private const val STATUS_COMPLETED = "COMPLETED"

/*
 * Every field is nullable on purpose: Gson fills these from whatever file the user picks, and
 * it happily leaves a missing key null even where Kotlin says it can't be. Reading them as
 * nullable makes each gap explicit — optional fields fall back to their defaults (older
 * backups lack newer fields), required ones refuse the whole file (see validate()).
 */

private data class BackupSnapshotDto(
    val version: Int? = BACKUP_FORMAT_VERSION,
    val exportedAt: Long? = System.currentTimeMillis(),
    val customExercises: List<ExerciseDto>? = emptyList(),
    val routines: List<RoutineDto>? = emptyList(),
    val sessions: List<SessionDto>? = emptyList(),
    val bodyWeightEntries: List<BodyWeightDto>? = emptyList(),
)

/** Exercises are cross-referenced by [name], not row id, so a snapshot is portable across installs. */
private data class ExerciseDto(
    val name: String?,
    val description: String? = "",
    val instructions: String? = "",
    val muscleGroup: String? = "",
    val exerciseType: String? = "",
    val equipment: String? = "",
    /** Raw Gson-encoded fractional muscle weights, carried through as-is — see Exercise.muscleWeights. */
    val muscleWeightsJson: String? = "",
)

private data class RoutineDto(
    val name: String?,
    val description: String? = "",
    val type: String? = "GYM",
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = System.currentTimeMillis(),
    val exercises: List<RoutineExerciseDto>? = emptyList(),
)

private data class RoutineExerciseDto(
    val exerciseName: String?,
    val orderIndex: Int? = 0,
    val targetSets: Int? = 3,
    val targetReps: Int? = 10,
    val targetWeight: Float? = 0f,
    val targetDurationSecs: Int? = 0,
    val restBetweenSetsSecs: Int? = 60,
    val notes: String? = "",
    val progressionPolicy: String? = "LINEAR",
    val repRangeLow: Int? = 8,
    val repRangeHigh: Int? = 10,
    val stallThreshold: Int? = 3,
    /** Preserved verbatim — still valid, since exercises re-import in the same order. */
    val supersetGroupId: String? = null,
    val weightUnit: String? = null,
)

/** routineId / [endTime] intentionally NOT linked back to a routine — see BackupRepository doc. */
private data class SessionDto(
    val sessionName: String? = "",
    val startTime: Long?,
    val endTime: Long? = null,
    val totalDurationSecs: Int? = 0,
    val status: String? = STATUS_COMPLETED,
    val notes: String? = "",
    val type: String? = "GYM",
    val sets: List<SetDto>? = emptyList(),
)

private data class SetDto(
    val exerciseName: String?,
    val setNumber: Int? = 1,
    val reps: Int? = null,
    val weight: Float? = null,
    val durationSecs: Int? = null,
    val distance: Float? = null,
    val isCompleted: Boolean? = true,
    val completedAt: Long? = null,
    val notes: String? = "",
    val isWarmup: Boolean? = false,
    val effortType: String? = null,
    val effortValue: Float? = null,
    val weightUnit: String? = null,
)

private data class BodyWeightDto(val weightKg: Float?, val loggedAt: Long?)

private fun invalid(reason: String): Nothing = throw IllegalArgumentException("Archivo de respaldo inválido: $reason")

private inline fun <reified E : Enum<E>> String?.requireIn(field: String) {
    if (this == null || enumValues<E>().none { it.name == this }) invalid("$field desconocido: $this")
}

/**
 * Checks the whole snapshot before anything is written: a value the app can't read back (an
 * unknown muscle group or workout status, a routine without a name…) would otherwise be stored
 * and then crash every screen that lists it.
 */
private fun BackupSnapshotDto.validate() {
    if ((version ?: 0) > BACKUP_FORMAT_VERSION) invalid("hecho con una versión más nueva de la app")
    customExercises.orEmpty().forEach {
        if (it.name.isNullOrBlank()) invalid("ejercicio sin nombre")
        it.muscleGroup.requireIn<MuscleGroup>("grupo muscular")
        it.exerciseType.requireIn<ExerciseType>("tipo de ejercicio")
    }
    routines.orEmpty().forEach { routine ->
        if (routine.name == null) invalid("rutina sin nombre")
        (routine.type ?: "GYM").requireIn<WorkoutType>("tipo de rutina")
        routine.exercises.orEmpty().forEach {
            if (it.exerciseName == null) invalid("ejercicio de rutina sin nombre")
            (it.progressionPolicy ?: "LINEAR").requireIn<ProgressionPolicy>("progresión")
        }
    }
    sessions.orEmpty().forEach { session ->
        if (session.startTime == null) invalid("entrenamiento sin fecha")
        (session.status ?: STATUS_COMPLETED).requireIn<WorkoutStatus>("estado de entrenamiento")
        (session.type ?: "GYM").requireIn<WorkoutType>("tipo de entrenamiento")
        session.sets.orEmpty().forEach { if (it.exerciseName == null) invalid("serie sin ejercicio") }
    }
    bodyWeightEntries.orEmpty().forEach {
        val kg = it.weightKg
        if (kg == null || !kg.isFinite() || kg <= 0f || it.loggedAt == null) invalid("peso corporal inválido")
    }
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: HeavyDutyDatabase,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao,
    private val bodyWeightDao: BodyWeightDao,
) : BackupRepository {

    private val gson = Gson()

    override suspend fun exportSnapshot(): String {
        val exercises = exerciseDao.getAllExercises().first()
        // Catalog exercises are keyed by their original English name: it never changes, while
        // the displayed (Spanish) name may be re-translated in a later version.
        val exerciseNameById = exercises.associate { it.id to it.nameEn.ifBlank { it.name } }

        val customExercises = exercises.filter { it.isCustom }.map {
            ExerciseDto(
                name = it.name,
                description = it.description,
                instructions = it.instructions,
                muscleGroup = it.muscleGroup,
                exerciseType = it.exerciseType,
                equipment = it.equipment,
                muscleWeightsJson = it.muscleWeightsJson,
            )
        }

        val routines = routineDao.getAllRoutines().first().map { routineWithExercises ->
            RoutineDto(
                name = routineWithExercises.routine.name,
                description = routineWithExercises.routine.description,
                type = routineWithExercises.routine.type,
                createdAt = routineWithExercises.routine.createdAt,
                updatedAt = routineWithExercises.routine.updatedAt,
                exercises = routineWithExercises.routineExercises.map { re ->
                    val rx = re.routineExercise
                    RoutineExerciseDto(
                        // Same stable key as sets: the English catalog name, so a re-translated
                        // catalog still resolves it (it used to export the Spanish display name).
                        exerciseName = re.exercise.nameEn.ifBlank { re.exercise.name },
                        orderIndex = rx.orderIndex,
                        targetSets = rx.targetSets,
                        targetReps = rx.targetReps,
                        targetWeight = rx.targetWeight,
                        targetDurationSecs = rx.targetDurationSecs,
                        restBetweenSetsSecs = rx.restBetweenSetsSecs,
                        notes = rx.notes,
                        progressionPolicy = rx.progressionPolicy,
                        repRangeLow = rx.repRangeLow,
                        repRangeHigh = rx.repRangeHigh,
                        stallThreshold = rx.stallThreshold,
                        supersetGroupId = rx.supersetGroupId,
                        weightUnit = rx.weightUnit,
                    )
                },
            )
        }

        val setsBySessionId = workoutDao.getAllLoggedSets().first().groupBy { it.sessionId }
        val sessions = workoutDao.getAllSessions().first().map { session ->
            SessionDto(
                sessionName = session.sessionName,
                startTime = session.startTime,
                endTime = session.endTime,
                totalDurationSecs = session.totalDurationSecs,
                status = session.status,
                notes = session.notes,
                type = session.type,
                sets = setsBySessionId[session.id].orEmpty().mapNotNull { set ->
                    val exerciseName = exerciseNameById[set.exerciseId] ?: return@mapNotNull null
                    SetDto(
                        exerciseName = exerciseName,
                        setNumber = set.setNumber,
                        reps = set.reps,
                        weight = set.weight,
                        durationSecs = set.durationSecs,
                        distance = set.distance,
                        isCompleted = set.isCompleted,
                        completedAt = set.completedAt,
                        notes = set.notes,
                        isWarmup = set.isWarmup,
                        effortType = set.effortType,
                        effortValue = set.effortValue,
                        weightUnit = set.weightUnit,
                    )
                },
            )
        }

        val bodyWeightEntries = bodyWeightDao.getAllEntries().first().map { BodyWeightDto(it.weightKg, it.loggedAt) }

        val snapshot = BackupSnapshotDto(
            customExercises = customExercises,
            routines = routines,
            sessions = sessions,
            bodyWeightEntries = bodyWeightEntries,
        )
        return gson.toJson(snapshot)
    }

    override suspend fun importSnapshot(json: String): BackupImportResult {
        val snapshot = try {
            // Only an object that says which backup format it is: valid JSON from another app
            // would otherwise parse into an empty snapshot and "import" nothing successfully.
            val root = JsonParser.parseString(json)
            if (!root.isJsonObject || !root.asJsonObject.has("version")) invalid("no es un respaldo de HeavyDuty")
            gson.fromJson(root, BackupSnapshotDto::class.java) ?: invalid("vacío")
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Archivo de respaldo inválido", e)
        }
        snapshot.validate()

        // All or nothing: a failure part-way must not leave half a backup behind.
        return database.withTransaction { write(snapshot) }
    }

    private suspend fun write(snapshot: BackupSnapshotDto): BackupImportResult {
        var skipped = 0
        // Importing a backup twice (or onto the phone it came from) must not duplicate anything:
        // a routine is the same if it has the same name and creation time, a workout if it
        // started at the same millisecond, a weigh-in if it's the same weight at the same time.
        var alreadyPresent = 0
        val existingRoutines = routineDao.getAllRoutines().first().mapTo(HashSet()) { it.routine.name to it.routine.createdAt }
        val existingSessionStarts = workoutDao.getAllSessions().first().mapTo(HashSet()) { it.startTime }
        val existingWeighIns = bodyWeightDao.getAllEntries().first().mapTo(HashSet()) { it.loggedAt to it.weightKg }

        val existingNames = exerciseDao.getAllExercises().first().map { it.name }.toMutableSet()
        var customExercisesImported = 0
        for (dto in snapshot.customExercises.orEmpty()) {
            val name = dto.name!!
            if (name in existingNames) continue
            exerciseDao.insertExercise(
                ExerciseEntity(
                    name = name,
                    description = dto.description.orEmpty(),
                    instructions = dto.instructions.orEmpty(),
                    muscleGroup = dto.muscleGroup!!,
                    exerciseType = dto.exerciseType!!,
                    equipment = dto.equipment.orEmpty(),
                    isCustom = true,
                    muscleWeightsJson = dto.muscleWeightsJson.orEmpty(),
                ),
            )
            existingNames.add(name)
            customExercisesImported++
        }

        // Re-read so newly-inserted custom exercises resolve by name below.
        // Backups name exercises in English (catalog key) or by their displayed name (older
        // backups, custom exercises) — accept either.
        val exerciseIdByName = exerciseDao.getAllExercises().first().flatMap { exercise ->
            listOfNotNull(exercise.name to exercise.id, exercise.nameEn.takeIf { it.isNotBlank() }?.let { it to exercise.id })
        }.toMap()

        var routinesImported = 0
        for (routineDto in snapshot.routines.orEmpty()) {
            val now = System.currentTimeMillis()
            val createdAt = routineDto.createdAt ?: now
            if (!existingRoutines.add(routineDto.name!! to createdAt)) {
                alreadyPresent++
                continue
            }
            val routineExercises = routineDto.exercises.orEmpty().mapNotNull { reDto ->
                val exerciseId = exerciseIdByName[reDto.exerciseName]
                if (exerciseId == null) {
                    skipped++
                    return@mapNotNull null
                }
                RoutineExerciseEntity(
                    routineId = 0,
                    exerciseId = exerciseId,
                    orderIndex = reDto.orderIndex ?: 0,
                    targetSets = reDto.targetSets ?: 3,
                    targetReps = reDto.targetReps ?: 10,
                    targetWeight = reDto.targetWeight ?: 0f,
                    targetDurationSecs = reDto.targetDurationSecs ?: 0,
                    restBetweenSetsSecs = reDto.restBetweenSetsSecs ?: 60,
                    notes = reDto.notes.orEmpty(),
                    progressionPolicy = reDto.progressionPolicy ?: "LINEAR",
                    repRangeLow = reDto.repRangeLow ?: 8,
                    repRangeHigh = reDto.repRangeHigh ?: 10,
                    stallThreshold = reDto.stallThreshold ?: 3,
                    supersetGroupId = reDto.supersetGroupId,
                    weightUnit = reDto.weightUnit,
                )
            }
            val routineId = routineDao.insertRoutine(
                RoutineEntity(
                    name = routineDto.name!!,
                    description = routineDto.description.orEmpty(),
                    type = routineDto.type ?: "GYM",
                    createdAt = createdAt,
                    updatedAt = routineDto.updatedAt ?: now,
                ),
            )
            routineExercises.forEach { routineDao.insertRoutineExercise(it.copy(routineId = routineId)) }
            routinesImported++
        }

        var sessionsImported = 0
        for (sessionDto in snapshot.sessions.orEmpty()) {
            val startTime = sessionDto.startTime!!
            if (!existingSessionStarts.add(startTime)) {
                alreadyPresent++
                continue
            }
            val sets = sessionDto.sets.orEmpty()
            // A workout still running when the backup was made comes back finished at its last
            // set: imported as running, it would take over the workout screen on this install.
            val wasRunning = sessionDto.status == STATUS_IN_PROGRESS
            val endTime = if (wasRunning) {
                sets.mapNotNull { it.completedAt }.maxOrNull() ?: startTime
            } else {
                sessionDto.endTime
            }
            val sessionId = workoutDao.insertSession(
                WorkoutSessionEntity(
                    routineId = null,
                    sessionName = sessionDto.sessionName.orEmpty(),
                    startTime = startTime,
                    endTime = endTime,
                    totalDurationSecs = if (wasRunning) {
                        ((endTime!! - startTime) / 1_000).toInt().coerceAtLeast(0)
                    } else {
                        sessionDto.totalDurationSecs ?: 0
                    },
                    status = if (wasRunning) STATUS_COMPLETED else sessionDto.status ?: STATUS_COMPLETED,
                    notes = sessionDto.notes.orEmpty(),
                    type = sessionDto.type ?: "GYM",
                ),
            )
            sets.forEach { setDto ->
                val exerciseId = exerciseIdByName[setDto.exerciseName]
                if (exerciseId == null) {
                    skipped++
                    return@forEach
                }
                workoutDao.insertSet(
                    WorkoutSetEntity(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setNumber = setDto.setNumber ?: 1,
                        reps = setDto.reps,
                        weight = setDto.weight,
                        durationSecs = setDto.durationSecs,
                        distance = setDto.distance,
                        isCompleted = setDto.isCompleted ?: true,
                        completedAt = setDto.completedAt,
                        notes = setDto.notes.orEmpty(),
                        isWarmup = setDto.isWarmup ?: false,
                        effortType = setDto.effortType,
                        effortValue = setDto.effortValue,
                        weightUnit = setDto.weightUnit,
                    ),
                )
            }
            sessionsImported++
        }

        var bodyWeightImported = 0
        snapshot.bodyWeightEntries.orEmpty().forEach { dto ->
            if (!existingWeighIns.add(dto.loggedAt!! to dto.weightKg!!)) {
                alreadyPresent++
                return@forEach
            }
            bodyWeightDao.insertEntry(BodyWeightEntity(weightKg = dto.weightKg!!, loggedAt = dto.loggedAt!!))
            bodyWeightImported++
        }

        return BackupImportResult(
            customExercisesImported = customExercisesImported,
            routinesImported = routinesImported,
            sessionsImported = sessionsImported,
            bodyWeightEntriesImported = bodyWeightImported,
            skippedReferences = skipped,
            alreadyPresent = alreadyPresent,
        )
    }
}
