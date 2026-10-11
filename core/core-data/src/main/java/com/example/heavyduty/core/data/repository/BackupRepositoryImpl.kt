package com.example.heavyduty.core.data.repository

import androidx.room.withTransaction
import com.example.heavyduty.core.data.database.HeavyDutyDatabase
import com.example.heavyduty.core.data.database.dao.BodyWeightDao
import com.example.heavyduty.core.data.database.dao.ExerciseDao
import com.example.heavyduty.core.data.database.dao.RoutineDao
import com.example.heavyduty.core.data.database.dao.WorkoutDao
import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.repository.BackupImportResult
import com.example.heavyduty.core.domain.repository.BackupRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: HeavyDutyDatabase,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao,
    private val bodyWeightDao: BodyWeightDao,
    private val clock: Clock
) : BackupRepository {

    private val gson = Gson()

    override suspend fun exportSnapshot(): String {
        val exercises = exerciseDao.getAllExercises().first()
        // Catalog exercises are keyed by their original English name: it never changes, while
        // the displayed (Spanish) name may be re-translated in a later version.
        val exerciseNameById = exercises.associate { it.id to it.nameEn.ifBlank { it.name } }

        val setsBySessionId = workoutDao.getAllLoggedSets().first().groupBy { it.sessionId }
        val snapshot = BackupSnapshotDto(
            customExercises = exercises.filter { it.isCustom }.map { it.toDto() },
            routines = routineDao.getAllRoutines().first().map { it.toDto() },
            sessions = workoutDao.getAllSessions().first().map { it.toDto(setsBySessionId[it.id].orEmpty(), exerciseNameById) },
            bodyWeightEntries = bodyWeightDao.getAllEntries().first().map { BodyWeightDto(it.weightKg, it.loggedAt) }
        )
        return gson.toJson(snapshot)
    }

    // Whatever the parser throws on a file that isn't a backup becomes the one error the
    // screen knows how to show.
    @Suppress("TooGenericExceptionCaught")
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
        // Importing a backup twice (or onto the phone it came from) must not duplicate anything:
        // a routine is the same if it has the same name and creation time, a workout if it
        // started at the same millisecond, a weigh-in if it's the same weight at the same time.
        val tally = ImportTally()
        val customExercisesImported = importCustomExercises(snapshot.customExercises.orEmpty())
        val exerciseIdByName = exerciseIdsByName()
        return BackupImportResult(
            customExercisesImported = customExercisesImported,
            routinesImported = importRoutines(snapshot.routines.orEmpty(), exerciseIdByName, tally),
            sessionsImported = importSessions(snapshot.sessions.orEmpty(), exerciseIdByName, tally),
            bodyWeightEntriesImported = importBodyWeight(snapshot.bodyWeightEntries.orEmpty(), tally),
            skippedReferences = tally.skipped,
            alreadyPresent = tally.alreadyPresent
        )
    }

    /** What the import left alone: references to unknown exercises, and rows already here. */
    private class ImportTally {
        var skipped = 0
        var alreadyPresent = 0
    }

    /** Custom exercises not already present by name. */
    private suspend fun importCustomExercises(dtos: List<ExerciseDto>): Int {
        val existingNames = exerciseDao.getAllExercises().first().map { it.name }.toMutableSet()
        var imported = 0
        for (dto in dtos) {
            val entity = dto.toEntity()
            if (!existingNames.add(entity.name)) continue
            exerciseDao.insertExercise(entity)
            imported++
        }
        return imported
    }

    private suspend fun exerciseIdsByName(): Map<String, Long> {
        // Re-read so newly-inserted custom exercises resolve by name below.
        // Backups name exercises in English (catalog key) or by their displayed name (older
        // backups, custom exercises) — accept either.
        return exerciseDao.getAllExercises().first().flatMap { exercise ->
            listOfNotNull(exercise.name to exercise.id, exercise.nameEn.takeIf { it.isNotBlank() }?.let { it to exercise.id })
        }.toMap()
    }

    private suspend fun importRoutines(dtos: List<RoutineDto>, exerciseIdByName: Map<String, Long>, tally: ImportTally): Int {
        val existingRoutines = routineDao.getAllRoutines().first().mapTo(HashSet()) { it.routine.name to it.routine.createdAt }
        var imported = 0
        for (routineDto in dtos) {
            val now = clock.nowMillis()
            val createdAt = routineDto.createdAt ?: now
            if (!existingRoutines.add(requireNotNull(routineDto.name) to createdAt)) {
                tally.alreadyPresent++
                continue
            }
            val routineExercises = routineDto.exercises.orEmpty().mapNotNull { reDto ->
                val exerciseId = exerciseIdByName[reDto.exerciseName] ?: run {
                    tally.skipped++
                    return@mapNotNull null
                }
                reDto.toEntity(exerciseId)
            }
            val routineId = routineDao.insertRoutine(routineDto.toEntity(createdAt = createdAt, now = now))
            routineExercises.forEach { routineDao.insertRoutineExercise(it.copy(routineId = routineId)) }
            imported++
        }
        return imported
    }

    private suspend fun importSessions(dtos: List<SessionDto>, exerciseIdByName: Map<String, Long>, tally: ImportTally): Int {
        val existingSessionStarts = workoutDao.getAllSessions().first().mapTo(HashSet()) { it.startTime }
        var imported = 0
        for (sessionDto in dtos) {
            if (!existingSessionStarts.add(requireNotNull(sessionDto.startTime))) {
                tally.alreadyPresent++
                continue
            }
            val sessionId = workoutDao.insertSession(sessionDto.toEntity())
            sessionDto.sets.orEmpty().forEach { setDto ->
                val exerciseId = exerciseIdByName[setDto.exerciseName] ?: run {
                    tally.skipped++
                    return@forEach
                }
                workoutDao.insertSet(setDto.toEntity(sessionId, exerciseId))
            }
            imported++
        }
        return imported
    }

    private suspend fun importBodyWeight(dtos: List<BodyWeightDto>, tally: ImportTally): Int {
        val existingWeighIns = bodyWeightDao.getAllEntries().first().mapTo(HashSet()) { it.loggedAt to it.weightKg }
        var imported = 0
        for (dto in dtos) {
            if (!existingWeighIns.add(dto.loggedAt!! to dto.weightKg!!)) {
                tally.alreadyPresent++
                continue
            }
            bodyWeightDao.insertEntry(BodyWeightEntity(weightKg = dto.weightKg, loggedAt = dto.loggedAt))
            imported++
        }
        return imported
    }
}
