package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.feature.dashboard.domain.model.BodyWeightSummaryUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val CHART_WINDOW_DAYS = 90L

class GetBodyWeightSummaryUseCase @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<BodyWeightSummaryUi> {
        val now = clock.nowMillis()
        val start = now - TimeUnit.DAYS.toMillis(CHART_WINDOW_DAYS)

        return combine(
            bodyWeightRepository.getEntriesByDateRange(start, now),
            userPreferencesRepository.goalWeightKg,
        ) { entries, goalKg ->
            val sorted = entries.sortedBy { it.loggedAt }
            BodyWeightSummaryUi(
                currentKg = sorted.lastOrNull()?.weightKg,
                deltaKg = if (sorted.size >= 2) {
                    sorted.last().weightKg - sorted[sorted.size - 2].weightKg
                } else {
                    null
                },
                goalKg = goalKg,
                chartPoints = sorted.map { it.loggedAt to it.weightKg },
            )
        }
    }
}
