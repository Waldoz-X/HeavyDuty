package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.common.dispatchers.DefaultDispatcher
import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.domain.model.WorkoutSession
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.WorkoutStatus
import com.example.heavyduty.core.domain.model.displayName
import com.example.heavyduty.core.domain.model.excludeWarmups
import com.example.heavyduty.core.domain.model.muscleCredits
import com.example.heavyduty.core.domain.repository.ExerciseRepository
import com.example.heavyduty.core.domain.repository.TrainingHistoryRepository
import com.example.heavyduty.feature.stats.domain.model.MonthlyReport
import com.example.heavyduty.feature.stats.domain.model.MuscleVolumeContribution
import com.example.heavyduty.feature.stats.domain.model.TopExerciseMonthly
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * Aggregates workouts, sets, volume and the most-trained muscle group for the
 * calendar month containing `referenceMillis` (defaults to the current month).
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val exerciseRepository: ExerciseRepository,
    private val clock: Clock,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    operator fun invoke(referenceMillis: Long = clock.nowMillis()): Flow<MonthlyReport> {
        val month = MonthBounds.containing(referenceMillis)
        return combine(
            trainingHistoryRepository.getSessionsByDateRange(month.start, month.end),
            trainingHistoryRepository.getCompletedSetsByDateRange(month.start, month.end),
            trainingHistoryRepository.getSessionsByDateRange(month.previousStart, month.previousEnd),
            trainingHistoryRepository.getCompletedSetsByDateRange(month.previousStart, month.previousEnd),
            exerciseRepository.getAllExercises()
        ) { sessions, rawSets, prevSessions, prevRawSets, exercises ->
            val current = MonthData.of(sessions, rawSets)
            val previous = MonthData.of(prevSessions, prevRawSets)
            buildReport(month, referenceMillis, current, previous, exercises.associateBy { it.id }, weeksSoFar(month, clock.nowMillis()))
        }.distinctUntilChanged().flowOn(defaultDispatcher)
    }
}

/** The month containing a moment, and the one before it (for the comparisons). */
private data class MonthBounds(val start: Long, val end: Long, val previousStart: Long, val weeks: Int, val days: Int) {
    val previousEnd: Long get() = start - 1

    companion object {
        fun containing(referenceMillis: Long): MonthBounds {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = referenceMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = calendar.timeInMillis
            val weeks = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH)
            val days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            // The month's last millisecond — "23:59:59" alone would drop its final second.
            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis - 1
            calendar.timeInMillis = start
            calendar.add(Calendar.MONTH, -1)
            return MonthBounds(start = start, end = end, previousStart = calendar.timeInMillis, weeks = weeks, days = days)
        }
    }
}

/** A month's finished workouts and their working sets (warm-ups and unfinished workouts left out). */
private data class MonthData(val sessionsById: Map<Long, WorkoutSession>, val sets: List<WorkoutSet>) {
    val sessions: Collection<WorkoutSession> get() = sessionsById.values

    companion object {
        fun of(sessions: List<WorkoutSession>, rawSets: List<WorkoutSet>): MonthData {
            val sessionsById = sessions.filter { it.status == WorkoutStatus.COMPLETED }.associateBy { it.id }
            return MonthData(sessionsById, rawSets.excludeWarmups().filter { it.sessionId in sessionsById })
        }
    }
}

private fun WorkoutSet.volumeKg(): Float = (reps ?: 0) * (weight ?: 0f)

private fun buildReport(
    month: MonthBounds,
    referenceMillis: Long,
    current: MonthData,
    previous: MonthData,
    exerciseById: Map<Long, Exercise>,
    weeksSoFar: Float
): MonthlyReport {
    // Only sets of exercises still in the catalog count, as in the per-muscle and top lists.
    val countedSets = current.sets.filter { it.exerciseId in exerciseById }
    val totalVolumeKg = countedSets.fold(0f) { total, set -> total + set.volumeKg() }
    val prevTotalVolumeKg = previous.sets.sumOf { it.volumeKg().toDouble() }.toFloat()
    val volumeByMuscle = volumeByMuscle(countedSets, exerciseById)
    val workoutCount = current.sessions.size
    val totalDurationSecs = current.sessions.sumOf { it.totalDurationSecs }

    return MonthlyReport(
        monthStartMillis = month.start,
        referenceMillis = referenceMillis,
        totalWorkouts = workoutCount,
        totalSets = current.sets.size,
        totalVolumeKg = totalVolumeKg,
        totalDurationMinutes = totalDurationSecs / SECONDS_PER_MINUTE,
        avgSessionDurationMinutes = if (workoutCount > 0) totalDurationSecs / workoutCount / SECONDS_PER_MINUTE else 0,
        workoutsPerWeekAverage = if (weeksSoFar > 0f) workoutCount / weeksSoFar else 0f,
        prevMonthWorkoutsDelta = (workoutCount - previous.sessions.size).takeIf { workoutCount > 0 || previous.sessions.isNotEmpty() },
        prevMonthVolumeDeltaPercent = if (prevTotalVolumeKg >
            0f
        ) {
            (totalVolumeKg - prevTotalVolumeKg) / prevTotalVolumeKg * PERCENT
        } else {
            null
        },
        prevMonthVolumeKg = prevTotalVolumeKg,
        topMuscleGroup = volumeByMuscle.maxByOrNull { it.value }?.key,
        muscleContributions = muscleContributions(volumeByMuscle, totalVolumeKg),
        topExercises = topExercises(current.sets, exerciseById),
        workoutsPerWeek = perWeek(month.weeks, current.sessions.groupingBy { weekOfMonth(it.startTime) }.eachCount(), 0),
        volumePerWeek = perWeek(month.weeks, volumePerWeek(countedSets, current.sessionsById), 0f)
    )
}

/**
 * How many weeks of the month have gone by at [nowMillis]: all of it for a past month, the days
 * so far for the current one — dividing a month in progress by all its weeks made 8 workouts in
 * 13 days read as 1.6 a week. At least one week, so the first days don't read as 7 a week.
 */
private fun weeksSoFar(month: MonthBounds, nowMillis: Long): Float {
    val daysSoFar = when {
        nowMillis > month.end -> month.days
        nowMillis < month.start -> return 0f
        else -> Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.DAY_OF_MONTH)
    }
    return (daysSoFar / DAYS_PER_WEEK).coerceAtLeast(1f)
}

private const val DAYS_PER_WEEK = 7f

private fun weekOfMonth(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.WEEK_OF_MONTH)

/** Every week of the month, 1 to [weeks], with the empty ones at [none]. */
private fun <T> perWeek(weeks: Int, byWeek: Map<Int, T>, none: T): List<Pair<Int, T>> = (1..weeks).map { it to (byWeek[it] ?: none) }

private fun volumePerWeek(sets: List<WorkoutSet>, sessionsById: Map<Long, WorkoutSession>): Map<Int, Float> {
    val volume = mutableMapOf<Int, Float>()
    sets.forEach { set ->
        val session = sessionsById[set.sessionId] ?: return@forEach
        val week = weekOfMonth(session.startTime)
        volume[week] = (volume[week] ?: 0f) + set.volumeKg()
    }
    return volume
}

private fun volumeByMuscle(sets: List<WorkoutSet>, exerciseById: Map<Long, Exercise>): Map<MuscleGroup, Float> {
    val volume = mutableMapOf<MuscleGroup, Float>()
    sets.forEach { set ->
        val exercise = exerciseById[set.exerciseId] ?: return@forEach
        exercise.muscleCredits(set.volumeKg()).forEach { (muscle, credit) ->
            volume[muscle] = (volume[muscle] ?: 0f) + credit
        }
    }
    return volume
}

private fun muscleContributions(volumeByMuscle: Map<MuscleGroup, Float>, totalVolumeKg: Float): List<MuscleVolumeContribution> {
    if (totalVolumeKg <= 0f) return emptyList()
    return volumeByMuscle.entries
        .filter { it.value > 0f }
        .sortedByDescending { it.value }
        .map {
            MuscleVolumeContribution(
                muscleGroup = it.key,
                volumeKg = it.value,
                percentage = (it.value / totalVolumeKg * PERCENT).coerceIn(0f, PERCENT)
            )
        }
}

private fun topExercises(sets: List<WorkoutSet>, exerciseById: Map<Long, Exercise>): List<TopExerciseMonthly> = sets
    .groupBy { it.exerciseId }
    .mapNotNull { (exerciseId, exSets) ->
        val exercise = exerciseById[exerciseId] ?: return@mapNotNull null
        TopExerciseMonthly(
            exerciseId = exerciseId,
            exerciseName = exercise.displayName,
            muscleGroup = exercise.muscleGroup,
            totalSets = exSets.size,
            totalVolumeKg = exSets.sumOf { it.volumeKg().toDouble() }.toFloat(),
            maxWeightKg = exSets.maxOfOrNull { it.weight ?: 0f } ?: 0f
        )
    }
    .sortedByDescending { it.totalVolumeKg }
    .take(TOP_EXERCISES)

private const val SECONDS_PER_MINUTE = 60
private const val PERCENT = 100f
private const val TOP_EXERCISES = 5
