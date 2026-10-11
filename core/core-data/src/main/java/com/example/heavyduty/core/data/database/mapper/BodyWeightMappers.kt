package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.BodyWeightEntity
import com.example.heavyduty.core.domain.model.BodyWeightEntry

fun BodyWeightEntity.toDomain(): BodyWeightEntry = BodyWeightEntry(
    id = id,
    weightKg = weightKg,
    loggedAt = loggedAt
)
