package com.example.heavyduty.core.data.database.mapper

import com.example.heavyduty.core.data.database.entity.CalendarEntryEntity
import com.example.heavyduty.core.domain.model.CalendarEntry
import com.example.heavyduty.core.domain.model.Recurrence

fun CalendarEntryEntity.toDomain(): CalendarEntry = CalendarEntry(
    id = id,
    routineId = routineId,
    dayOfWeek = dayOfWeek,
    recurrence = recurrence.toEnumOr(Recurrence.WEEKLY),
    isActive = isActive
)

fun CalendarEntry.toEntity(): CalendarEntryEntity = CalendarEntryEntity(
    id = id,
    routineId = routineId,
    dayOfWeek = dayOfWeek,
    recurrence = recurrence.name,
    isActive = isActive
)
