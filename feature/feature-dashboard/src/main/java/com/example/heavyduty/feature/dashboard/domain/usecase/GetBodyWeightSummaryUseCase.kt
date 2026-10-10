package com.example.heavyduty.feature.dashboard.domain.usecase

import com.example.heavyduty.core.domain.model.Clock
import com.example.heavyduty.core.domain.repository.BodyWeightRepository
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import com.example.heavyduty.feature.dashboard.domain.model.BodyWeightSummaryUi
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val CHART_WINDOW_DAYS = 90L

class GetBodyWeightSummaryUseCase @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<BodyWeightSummaryUi> {
        val start = clock.nowMillis() - TimeUnit.DAYS.toMillis(CHART_WINDOW_DAYS)

        return combine(
            // Open-ended: closing the range at "now" (fixed when the card starts watching) left
            // out a weigh-in logged afterwards — even from the dashboard's own sheet.
            bodyWeightRepository.getEntriesByDateRange(start, Long.MAX_VALUE),
            userPreferencesRepository.goalWeightKg
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
                chartPoints = sorted.map { it.loggedAt to it.weightKg }
            )
        }
    }
}
