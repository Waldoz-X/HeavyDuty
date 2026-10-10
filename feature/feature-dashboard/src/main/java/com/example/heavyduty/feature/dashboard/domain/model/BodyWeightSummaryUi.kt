package com.example.heavyduty.feature.dashboard.domain.model

data class BodyWeightSummaryUi(
    val currentKg: Float? = null,
    val deltaKg: Float? = null,
    val goalKg: Float? = null,
    val chartPoints: List<Pair<Long, Float>> = emptyList()
)
