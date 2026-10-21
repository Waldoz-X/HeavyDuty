package com.example.heavyduty.core.testing

import com.example.heavyduty.core.domain.model.BodyWeightEntry
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.ReminderSettings
import com.example.heavyduty.core.domain.model.Routine
import com.example.heavyduty.core.domain.model.RoutineExercise
import com.example.heavyduty.core.domain.model.ThemeMode
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.CalendarRepository
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.RoutineRepository
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.core.domain.repository.TrainingStatsFromHistory
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/*
 * In-memory repositories for JVM tests, shared by every module. Each answers its queries the
 * way the Room DAOs do (which sets count, how ranges are matched), so tests exercise use cases
 * and ViewModels against the data the app really hands them. Writes are kept, so a test can
 * act and then read back.
 */

class FakeWorkoutRepository(sessions: List<WorkoutSession> = emptyList(), sets: List<WorkoutSet> = emptyList()) : WorkoutRepository {
    val sessions = MutableStateFlow(sessions)
    val sets = MutableStateFlow(sets)

    /** The same workouts, as the history and stats screens read them. */
    val history = FakeTrainingHistoryRepository(this.sessions, this.sets)
    val stats = TrainingStatsFromHistory(history)

    override fun getSessionById(id: Long): Flow<WorkoutSession?> = sessions.map { list -> list.firstOrNull { it.id == id } }
    override fun getSessionWithSets(id: Long): Flow<WorkoutSession?> =
        combine(sessions, sets) { s, sets -> s.firstOrNull { it.id == id }?.copy(sets = sets.filter { it.sessionId == id }) }
    override fun getActiveSession() =
        sessions.map { list -> list.filter { it.status == WorkoutStatus.IN_PROGRESS }.maxByOrNull { it.startTime } }

    override suspend fun insertSession(session: WorkoutSession): Long {
        val id = if (session.id != 0L) session.id else (sessions.value.maxOfOrNull { it.id } ?: 0) + 1
        sessions.update { list -> list.filter { it.id != id } + session.copy(id = id) }
        return id
    }
    override suspend fun updateSession(session: WorkoutSession) = sessions.update { list ->
        list.map {
            if (it.id ==
                session.id
            ) {
                session
            } else {
                it
            }
        }
    }
    override suspend fun deleteSession(session: WorkoutSession) {
        sessions.update { list -> list.filter { it.id != session.id } }
        sets.update { list -> list.filter { it.sessionId != session.id } }
    }

    /** Like the DAO: a set inserted at a taken number moves the exercise's later sets up one. */
    override suspend fun insertSet(set: WorkoutSet): Long {
        val id = if (set.id != 0L) set.id else (sets.value.maxOfOrNull { it.id } ?: 0) + 1
        sets.update { list -> list.filter { it.id != id }.shiftedFrom(set, by = 1) + set.copy(id = id) }
        return id
    }
    override suspend fun updateSet(set: WorkoutSet) = sets.update { list -> list.map { if (it.id == set.id) set else it } }

    /** Like the DAO: the exercise's later sets in that workout move down one. */
    override suspend fun deleteSet(set: WorkoutSet) = sets.update { list -> list.filter { it.id != set.id }.shiftedFrom(set, by = -1) }
}

/** The sets of [set]'s exercise and workout numbered at or after it, renumbered by [by]. */
fun List<WorkoutSet>.shiftedFrom(set: WorkoutSet, by: Int): List<WorkoutSet> = map {
    val sameExercise = it.sessionId == set.sessionId && it.exerciseId == set.exerciseId && it.id != set.id
    val from = if (by > 0) it.setNumber >= set.setNumber else it.setNumber > set.setNumber
    if (sameExercise && from) it.copy(setNumber = it.setNumber + by) else it
}

/** Reads the workouts of a [FakeWorkoutRepository] (its [FakeWorkoutRepository.history]), or of the lists given. */
class FakeTrainingHistoryRepository(val sessions: MutableStateFlow<List<WorkoutSession>>, val sets: MutableStateFlow<List<WorkoutSet>>) :
    TrainingHistoryRepository {
    constructor(sessions: List<WorkoutSession> = emptyList(), sets: List<WorkoutSet> = emptyList()) :
        this(MutableStateFlow(sessions), MutableStateFlow(sets))

    /** Logged sets of finished workouts (WorkoutDao.getAllCompletedSets). */
    private fun counted(sessions: List<WorkoutSession>, sets: List<WorkoutSet>): List<Pair<WorkoutSet, WorkoutSession>> {
        val byId = sessions.associateBy { it.id }
        return sets.mapNotNull { set ->
            val session = byId[set.sessionId]?.takeIf { it.status == WorkoutStatus.COMPLETED } ?: return@mapNotNull null
            if (set.isCompleted) set to session else null
        }
    }

    private val both = combine(this.sessions, this.sets) { a, b -> a to b }

    override fun getAllSessions(): Flow<List<WorkoutSession>> = sessions.map { list -> list.sortedByDescending { it.startTime } }
    override fun getSessionsByDateRange(startTime: Long, endTime: Long) =
        sessions.map { list -> list.filter { it.startTime in startTime..endTime }.sortedByDescending { it.startTime } }
    override fun getCompletedSessionsByRoutineId(routineId: Long) =
        sessions.map { list -> list.filter { it.routineId == routineId && it.status == WorkoutStatus.COMPLETED }.sortedBy { it.startTime } }
    override fun getSetsByExerciseId(exerciseId: Long) =
        sets.map { list -> list.filter { it.exerciseId == exerciseId }.sortedByDescending { it.completedAt } }
    override fun getAllCompletedSets() = both.map { (s, sets) -> counted(s, sets).map { it.first } }
    override fun getCompletedSetsByExerciseIdBefore(exerciseId: Long, beforeStartTime: Long) = both.map { (s, sets) ->
        counted(s, sets).filter { (set, session) -> set.exerciseId == exerciseId && session.startTime < beforeStartTime }.map { it.first }
    }

    /** A set belongs to the period its workout started in (WorkoutDao.getCompletedSetsByDateRange). */
    override fun getCompletedSetsByDateRange(startTime: Long, endTime: Long) =
        both.map { (s, sets) -> counted(s, sets).filter { it.second.startTime in startTime..endTime }.map { it.first } }
}

class FakeExerciseRepository(exercises: List<Exercise> = emptyList()) : ExerciseRepository {
    val exercises = MutableStateFlow(exercises)

    override fun getAllExercises(): Flow<List<Exercise>> = exercises
    override fun getExerciseById(id: Long): Flow<Exercise?> = exercises.map { list -> list.firstOrNull { it.id == id } }
    override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup) =
        exercises.map { list -> list.filter { it.muscleGroup == muscleGroup } }
    override fun getExercisesByType(type: ExerciseType) = exercises.map { list -> list.filter { it.exerciseType == type } }
    override fun searchExercises(query: String) = exercises.map { list -> list.filter { it.name.contains(query, ignoreCase = true) } }
    override suspend fun insertExercise(exercise: Exercise): Long {
        val id = if (exercise.id != 0L) exercise.id else (exercises.value.maxOfOrNull { it.id } ?: 0) + 1
        exercises.update { it + exercise.copy(id = id) }
        return id
    }
    override suspend fun updateExercise(exercise: Exercise) = exercises.update { list ->
        list.map {
            if (it.id ==
                exercise.id
            ) {
                exercise
            } else {
                it
            }
        }
    }
    override suspend fun deleteExercise(exercise: Exercise) = exercises.update { list -> list.filter { it.id != exercise.id } }
}

class FakeRoutineRepository(routines: List<Routine> = emptyList()) : RoutineRepository {
    val routines = MutableStateFlow(routines)

    override fun getAllRoutines(): Flow<List<Routine>> = routines
    override fun getRoutineById(id: Long): Flow<Routine?> = routines.map { list -> list.firstOrNull { it.id == id } }
    override fun getRoutineWithExercises(id: Long): Flow<Routine?> = getRoutineById(id)
    override suspend fun insertRoutine(routine: Routine): Long {
        val id = if (routine.id != 0L) routine.id else (routines.value.maxOfOrNull { it.id } ?: 0) + 1
        routines.update { it + routine.copy(id = id) }
        return id
    }
    override suspend fun updateRoutine(routine: Routine) = routines.update { list -> list.map { if (it.id == routine.id) routine else it } }
    override suspend fun deleteRoutine(routine: Routine) = routines.update { list -> list.filter { it.id != routine.id } }
    override suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long = error("not supported by this fake")
    override suspend fun updateRoutineExercise(routineExercise: RoutineExercise) = error("not supported by this fake")
    override suspend fun deleteRoutineExercise(routineExercise: RoutineExercise) = error("not supported by this fake")
    override suspend fun updateRoutineExerciseOrder(routineId: Long, exercises: List<RoutineExercise>) = error("not supported by this fake")
}

/** Like CalendarDao: only active entries are listed, a week schedule is grouped by day. */
class FakeCalendarRepository(entries: List<CalendarEntry> = emptyList()) : CalendarRepository {
    val entries = MutableStateFlow(entries)

    private val active get() = entries.map { list -> list.filter { it.isActive }.sortedBy { it.dayOfWeek } }

    override fun getAllEntries(): Flow<List<CalendarEntry>> = active
    override fun getEntriesByDay(dayOfWeek: Int) = active.map { list -> list.filter { it.dayOfWeek == dayOfWeek } }
    override fun getWeekSchedule() = active.map { list -> list.groupBy { it.dayOfWeek } }
    override suspend fun hasRoutineOnDay(routineId: Long, dayOfWeek: Int) =
        entries.value.any { it.isActive && it.routineId == routineId && it.dayOfWeek == dayOfWeek }
    override suspend fun insertEntry(entry: CalendarEntry): Long {
        val id = if (entry.id != 0L) entry.id else (entries.value.maxOfOrNull { it.id } ?: 0) + 1
        entries.update { list -> list.filter { it.id != id } + entry.copy(id = id) }
        return id
    }
    override suspend fun updateEntry(entry: CalendarEntry) = entries.update { list -> list.map { if (it.id == entry.id) entry else it } }
    override suspend fun deleteEntry(entry: CalendarEntry) = entries.update { list -> list.filter { it.id != entry.id } }
    override suspend fun deleteEntriesByRoutineId(routineId: Long) = entries.update { list -> list.filter { it.routineId != routineId } }
}

class FakeBodyWeightRepository(entries: List<BodyWeightEntry> = emptyList()) : BodyWeightRepository {
    val entries = MutableStateFlow(entries)

    override fun getAllEntries(): Flow<List<BodyWeightEntry>> = entries.map { list -> list.sortedByDescending { it.loggedAt } }
    override fun getEntriesByDateRange(startTime: Long, endTime: Long) = entries.map { list ->
        list.filter {
            it.loggedAt in
                startTime..endTime
        }
    }
    override fun getLatestEntry(): Flow<BodyWeightEntry?> = entries.map { list -> list.maxByOrNull { it.loggedAt } }
    override suspend fun logEntry(weightKg: Float, loggedAt: Long): Long {
        val id = (entries.value.maxOfOrNull { it.id } ?: 0) + 1
        entries.update { it + BodyWeightEntry(id = id, weightKg = weightKg, loggedAt = loggedAt) }
        return id
    }
    override suspend fun deleteEntry(entry: BodyWeightEntry) = entries.update { list -> list.filter { it.id != entry.id } }
}

class FakeUserPreferencesRepository(goalWeightKg: Float? = null) : UserPreferencesRepository {
    override val displayName = MutableStateFlow("")
    override val bodyDiagramGender = MutableStateFlow("male")
    override val goalWeightKg = MutableStateFlow(goalWeightKg)
    override val effortTrackingEnabled = MutableStateFlow(false)
    override val preferredEffortScale = MutableStateFlow("RIR")
    override val preferredWeightUnit = MutableStateFlow(WeightUnit.KG)
    override val trainingReminder = MutableStateFlow(ReminderSettings())
    override val onboardingCompleted = MutableStateFlow(false)
    override val themeMode = MutableStateFlow(ThemeMode.DARK)

    override suspend fun setDisplayName(name: String) {
        displayName.value = name
    }
    override suspend fun setBodyDiagramGender(gender: String) {
        bodyDiagramGender.value = gender
    }
    override suspend fun setGoalWeightKg(kg: Float?) {
        goalWeightKg.value = kg
    }
    override suspend fun setEffortTrackingEnabled(enabled: Boolean) {
        effortTrackingEnabled.value = enabled
    }
    override suspend fun setPreferredEffortScale(scale: String) {
        preferredEffortScale.value = scale
    }
    override suspend fun setPreferredWeightUnit(unit: WeightUnit) {
        preferredWeightUnit.value = unit
    }
    override suspend fun setTrainingReminder(settings: ReminderSettings) {
        trainingReminder.value = settings
    }
    override suspend fun setOnboardingCompleted() {
        onboardingCompleted.value = true
    }
    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}
