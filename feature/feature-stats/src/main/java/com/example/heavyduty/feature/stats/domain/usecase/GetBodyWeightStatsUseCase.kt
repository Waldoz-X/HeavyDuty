package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

data class BodyWeightHistoryItem(val id: Long, val weightKg: Float, val loggedAt: Long, val deltaKg: Float?)

data class BodyWeightStatsReport(
    val maxWeightKg: Float?,
    val minWeightKg: Float?,
    val avgWeightKg: Float?,
    val goalWeightKg: Float?,
    val currentWeightKg: Float?,
    val startWeightKg: Float?,
    val progressPercentage: Float?,
    /** Left to the goal; past it, negative; at it (see [GOAL_REACHED_KG]), exactly 0. */
    val remainingToGoalKg: Float?,
    val chartPoints: List<Pair<Long, Float>>,
    val history: List<BodyWeightHistoryItem>
)

class GetBodyWeightStatsUseCase @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<BodyWeightStatsReport> = combine(
        bodyWeightRepository.getAllEntries(),
        userPreferencesRepository.goalWeightKg
    ) { entries, goalKg ->
        val sorted = entries.sortedBy { it.loggedAt }

        val max = sorted.maxOfOrNull { it.weightKg }
        val min = sorted.minOfOrNull { it.weightKg }
        val avg = if (sorted.isNotEmpty()) sorted.map { it.weightKg }.average().toFloat() else null
        val current = sorted.lastOrNull()?.weightKg
        val start = sorted.firstOrNull()?.weightKg

        val (progress, remaining) = if (goalKg != null && current != null && start != null) {
            if (start == goalKg) {
                val p = if (current == goalKg) 1f else 0f
                p to kotlin.math.abs(current - goalKg)
            } else if (goalKg < start) {
                // Objetivo: perder peso
                val totalDiff = start - goalKg
                val currentProgress = start - current
                val pct = (currentProgress / totalDiff).coerceIn(0f, 1f)
                val rem = current - goalKg
                pct to rem
            } else {
                // Objetivo: ganar peso
                val totalDiff = goalKg - start
                val currentProgress = current - start
                val pct = (currentProgress / totalDiff).coerceIn(0f, 1f)
                val rem = goalKg - current
                pct to rem
            }
        } else {
            null to null
        }.let { (pct, rem) ->
            // Closer than the 0.1 kg weights are shown in is the goal itself: 80.2 kg against
            // 80.0 is still 0.2 to go, while 80.03 reads as 80.0 and is done.
            if (rem != null && kotlin.math.abs(rem) < GOAL_REACHED_KG) 1f to 0f else pct to rem
        }

        val historyItems = sorted.mapIndexed { index, entry ->
            val delta = if (index > 0) entry.weightKg - sorted[index - 1].weightKg else null
            BodyWeightHistoryItem(
                id = entry.id,
                weightKg = entry.weightKg,
                loggedAt = entry.loggedAt,
                deltaKg = delta
            )
        }.reversed()

        BodyWeightStatsReport(
            maxWeightKg = max,
            minWeightKg = min,
            avgWeightKg = avg,
            goalWeightKg = goalKg,
            currentWeightKg = current,
            startWeightKg = start,
            progressPercentage = progress,
            remainingToGoalKg = remaining,
            chartPoints = sorted.map { it.loggedAt to it.weightKg },
            history = historyItems
        )
    }.distinctUntilChanged()
}

/** Half the 0.1 kg step weights are shown in. */
private const val GOAL_REACHED_KG = 0.05f
