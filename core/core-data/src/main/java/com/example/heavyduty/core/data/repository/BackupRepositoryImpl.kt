package com.example.heavyduty.core.data.repository

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
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_FORMAT_VERSION = 1

private data class BackupSnapshotDto(
    val version: Int = BACKUP_FORMAT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val customExercises: List<ExerciseDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val bodyWeightEntries: List<BodyWeightDto> = emptyList(),
)

/** Exercises are cross-referenced by [name], not row id, so a snapshot is portable across installs. */
private data class ExerciseDto(
    val name: String,
    val description: String = "",
    val instructions: String = "",
    val muscleGroup: String = "",
    val exerciseType: String = "",
    val equipment: String = "",
    /** Raw Gson-encoded fractional muscle weights, carried through as-is — see Exercise.muscleWeights. */
    val muscleWeightsJson: String = "",
)

private data class RoutineDto(
    val name: String,
    val description: String = "",
    val type: String = "GYM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val exercises: List<RoutineExerciseDto> = emptyList(),
)

private data class RoutineExerciseDto(
    val exerciseName: String,
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeight: Float = 0f,
    val targetDurationSecs: Int = 0,
    val restBetweenSetsSecs: Int = 60,
    val notes: String = "",
    val progressionPolicy: String = "LINEAR",
    val repRangeLow: Int = 8,
    val repRangeHigh: Int = 10,
    val stallThreshold: Int = 3,
    /** Preserved verbatim — still valid, since exercises re-import in the same order. */
    val supersetGroupId: String? = null,
    val weightUnit: String? = null,
)

/** routineId / [endTime] intentionally NOT linked back to a routine — see BackupRepository doc. */
private data class SessionDto(
    val sessionName: String = "",
    val startTime: Long,
    val endTime: Long? = null,
    val totalDurationSecs: Int = 0,
    val status: String = "COMPLETED",
    val notes: String = "",
    val type: String = "GYM",
    val sets: List<SetDto> = emptyList(),
)

private data class SetDto(
    val exerciseName: String,
    val setNumber: Int = 1,
    val reps: Int? = null,
    val weight: Float? = null,
    val durationSecs: Int? = null,
    val distance: Float? = null,
    val isCompleted: Boolean = true,
    val completedAt: Long? = null,
    val notes: String = "",
    val isWarmup: Boolean = false,
    val effortType: String? = null,
    val effortValue: Float? = null,
    val weightUnit: String? = null,
)

private data class BodyWeightDto(val weightKg: Float, val loggedAt: Long)

@Singleton
class BackupRepositoryImpl @Inject constructor(
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
                        exerciseName = re.exercise.name,
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
            gson.fromJson(json, BackupSnapshotDto::class.java) ?: throw IllegalArgumentException("Archivo vacío")
        } catch (e: Exception) {
            throw IllegalArgumentException("Archivo de respaldo inválido", e)
        }

        var skipped = 0

        val existingNames = exerciseDao.getAllExercises().first().map { it.name }.toMutableSet()
        var customExercisesImported = 0
        for (dto in snapshot.customExercises) {
            if (dto.name in existingNames) continue
            exerciseDao.insertExercise(
                ExerciseEntity(
                    name = dto.name,
                    description = dto.description,
                    instructions = dto.instructions,
                    muscleGroup = dto.muscleGroup,
                    exerciseType = dto.exerciseType,
                    equipment = dto.equipment,
                    isCustom = true,
                    muscleWeightsJson = dto.muscleWeightsJson,
                ),
            )
            existingNames.add(dto.name)
            customExercisesImported++
        }

        // Re-read so newly-inserted custom exercises resolve by name below.
        // Backups name exercises in English (catalog key) or by their displayed name (older
        // backups, custom exercises) — accept either.
        val exerciseIdByName = exerciseDao.getAllExercises().first().flatMap { exercise ->
            listOfNotNull(exercise.name to exercise.id, exercise.nameEn.takeIf { it.isNotBlank() }?.let { it to exercise.id })
        }.toMap()

        var routinesImported = 0
        for (routineDto in snapshot.routines) {
            val routineExercises = routineDto.exercises.mapNotNull { reDto ->
                val exerciseId = exerciseIdByName[reDto.exerciseName]
                if (exerciseId == null) {
                    skipped++
                    return@mapNotNull null
                }
                RoutineExerciseEntity(
                    routineId = 0,
                    exerciseId = exerciseId,
                    orderIndex = reDto.orderIndex,
                    targetSets = reDto.targetSets,
                    targetReps = reDto.targetReps,
                    targetWeight = reDto.targetWeight,
                    targetDurationSecs = reDto.targetDurationSecs,
                    restBetweenSetsSecs = reDto.restBetweenSetsSecs,
                    notes = reDto.notes,
                    progressionPolicy = reDto.progressionPolicy,
                    repRangeLow = reDto.repRangeLow,
                    repRangeHigh = reDto.repRangeHigh,
                    stallThreshold = reDto.stallThreshold,
                    supersetGroupId = reDto.supersetGroupId,
                    weightUnit = reDto.weightUnit,
                )
            }
            val routineId = routineDao.insertRoutine(
                RoutineEntity(
                    name = routineDto.name,
                    description = routineDto.description,
                    type = routineDto.type,
                    createdAt = routineDto.createdAt,
                    updatedAt = routineDto.updatedAt,
                ),
            )
            routineExercises.forEach { routineDao.insertRoutineExercise(it.copy(routineId = routineId)) }
            routinesImported++
        }

        var sessionsImported = 0
        for (sessionDto in snapshot.sessions) {
            val sessionId = workoutDao.insertSession(
                WorkoutSessionEntity(
                    routineId = null,
                    sessionName = sessionDto.sessionName,
                    startTime = sessionDto.startTime,
                    endTime = sessionDto.endTime,
                    totalDurationSecs = sessionDto.totalDurationSecs,
                    status = sessionDto.status,
                    notes = sessionDto.notes,
                    type = sessionDto.type,
                ),
            )
            sessionDto.sets.forEach { setDto ->
                val exerciseId = exerciseIdByName[setDto.exerciseName]
                if (exerciseId == null) {
                    skipped++
                    return@forEach
                }
                workoutDao.insertSet(
                    WorkoutSetEntity(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setNumber = setDto.setNumber,
                        reps = setDto.reps,
                        weight = setDto.weight,
                        durationSecs = setDto.durationSecs,
                        distance = setDto.distance,
                        isCompleted = setDto.isCompleted,
                        completedAt = setDto.completedAt,
                        notes = setDto.notes,
                        isWarmup = setDto.isWarmup,
                        effortType = setDto.effortType,
                        effortValue = setDto.effortValue,
                        weightUnit = setDto.weightUnit,
                    ),
                )
            }
            sessionsImported++
        }

        var bodyWeightImported = 0
        snapshot.bodyWeightEntries.forEach { dto ->
            bodyWeightDao.insertEntry(BodyWeightEntity(weightKg = dto.weightKg, loggedAt = dto.loggedAt))
            bodyWeightImported++
        }

        return BackupImportResult(
            customExercisesImported = customExercisesImported,
            routinesImported = routinesImported,
            sessionsImported = sessionsImported,
            bodyWeightEntriesImported = bodyWeightImported,
            skippedReferences = skipped,
        )
    }
}
