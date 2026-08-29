package com.example.heavyduty.core.domain.model

import java.util.UUID

/**
 * Derives superset groups from a routine's ordered exercises: a run of two or more
 * CONSECUTIVE entries sharing the same non-null [RoutineExercise.supersetGroupId] is one
 * group. The same id showing up again later, separated by a different id or null, starts a
 * new group — grouping is about adjacency in the list, not id equality, so a reorder can
 * never silently merge unrelated exercises into the same superset. A stale id left on a lone
 * exercise (no contiguous partner) isn't part of any group.
 */
fun List<RoutineExercise>.supersetGroups(): List<List<RoutineExercise>> {
    val groups = mutableListOf<List<RoutineExercise>>()
    var run = mutableListOf<RoutineExercise>()
    for (exercise in this) {
        if (run.isNotEmpty() && exercise.supersetGroupId != null && exercise.supersetGroupId == run.last().supersetGroupId) {
            run.add(exercise)
        } else {
            if (run.size >= 2) groups.add(run)
            run = mutableListOf(exercise)
        }
    }
    if (run.size >= 2) groups.add(run)
    return groups
}

private fun List<RoutineExercise>.contiguousSpan(index: Int): IntRange {
    val id = this[index].supersetGroupId ?: return index..index
    var start = index
    while (start - 1 >= 0 && this[start - 1].supersetGroupId == id) start--
    var end = index
    while (end + 1 < size && this[end + 1].supersetGroupId == id) end++
    return start..end
}

/**
 * Pairs the exercise at [index] with the one right after it, forming (or extending, or
 * merging two neighboring) superset groups. The whole contiguous span each side already
 * belongs to gets unified under one id, so pairing at the boundary of two existing supersets
 * merges them rather than leaving a broken run.
 */
fun List<RoutineExercise>.pairWithNext(index: Int): List<RoutineExercise> {
    if (index !in indices || index + 1 !in indices) return this
    val mergedSpan = contiguousSpan(index).first..contiguousSpan(index + 1).last
    val groupId = this[index].supersetGroupId ?: this[index + 1].supersetGroupId ?: UUID.randomUUID().toString()
    return mapIndexed { i, exercise -> if (i in mergedSpan) exercise.copy(supersetGroupId = groupId) else exercise }
}

/**
 * Removes the exercise at [index] from its superset. If that leaves the rest of the group
 * with a single member, that lone member is dissolved too — a superset needs at least two.
 */
fun List<RoutineExercise>.unpair(index: Int): List<RoutineExercise> {
    if (index !in indices || this[index].supersetGroupId == null) return this
    val span = contiguousSpan(index)
    val remaining = (span.first..span.last).filter { it != index }
    val dissolveRemaining = remaining.size == 1
    return mapIndexed { i, exercise ->
        when {
            i == index -> exercise.copy(supersetGroupId = null)
            dissolveRemaining && i == remaining.first() -> exercise.copy(supersetGroupId = null)
            else -> exercise
        }
    }
}
