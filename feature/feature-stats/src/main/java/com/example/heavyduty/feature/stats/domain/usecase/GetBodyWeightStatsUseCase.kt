package com.example.heavyduty.feature.stats.domain.usecase

import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class BodyWeightStatsReport(
    val maxWeightKg: Float?,
    val minWeightKg: Float?,
    val avgWeightKg: Float?,
    val goalWeightKg: Float?,
    val currentWeightKg: Float?,
    val chartPoints: List<Pair<Long, Float>>,
    val history: List<com.example.heavyduty.core.domain.model.BodyWeightEntry>
)

class GetBodyWeightStatsUseCase @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<BodyWeightStatsReport> {
        return combine(
            bodyWeightRepository.getAllEntries(),
            userPreferencesRepository.goalWeightKg,
        ) { entries, goalKg ->
            val sorted = entries.sortedBy { it.loggedAt }
            
            val max = sorted.maxOfOrNull { it.weightKg }
            val min = sorted.minOfOrNull { it.weightKg }
            val avg = if (sorted.isNotEmpty()) sorted.map { it.weightKg }.average().toFloat() else null
            
            BodyWeightStatsReport(
                maxWeightKg = max,
                minWeightKg = min,
                avgWeightKg = avg,
                goalWeightKg = goalKg,
                currentWeightKg = sorted.lastOrNull()?.weightKg,
                chartPoints = sorted.map { it.loggedAt to it.weightKg },
                history = sorted.reversed()
            )
        }.distinctUntilChanged()
    }
}
