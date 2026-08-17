package io.github.Gabaraydin.vira.domain.cycle

import java.time.LocalDate

// position == null covers both ad-hoc workouts (no program day) and workouts whose
// program day was later deleted — the caller resolves programDayId to a position and
// passes null when it can't, so this function excludes both the same way.
data class WorkoutForCycle(val position: Int?, val date: LocalDate, val startedAt: Long)

enum class DayCycleStatus { DONE, NEXT, SKIPPED, UPCOMING }

data class CycleDayState(val position: Int, val status: DayCycleStatus, val workoutDate: LocalDate?)

data class CycleResult(val cycleIndex: Int, val nextPosition: Int, val days: List<CycleDayState>)

data class CompletedCycle<T>(val cycleIndex: Int, val completedDays: Int, val totalDays: Int, val workouts: List<T>)

// The History screen's "grouped by cycle" list: every pass through the program, oldest
// first, including a still-in-progress final one. Uses the same wrap rule as
// computeCycle (a position repeating or going backwards starts a new cycle) but, unlike
// computeCycle, returns all of them rather than just deriving today's live position.
// Generic over T (rather than WorkoutForCycle directly) so the caller's real workout rows
// — id, day name, etc. — ride along instead of getting lost behind the cycle-math shape.
fun <T> groupCompletedCycles(
    dayCount: Int,
    items: List<T>,
    position: (T) -> Int?,
    date: (T) -> LocalDate,
    startedAt: (T) -> Long,
): List<CompletedCycle<T>> {
    require(dayCount >= 1) { "dayCount must be at least 1, was $dayCount" }
    items.forEach { item ->
        position(item)?.let {
            require(it in 0 until dayCount) { "position $it is out of range for dayCount $dayCount" }
        }
    }

    val ordered = items
        .filter { position(it) != null }
        .sortedWith(compareBy({ date(it) }, { startedAt(it) }))

    val cycles = mutableListOf<MutableList<T>>()
    var previousPosition = -1
    for (item in ordered) {
        val p = position(item)!!
        if (cycles.isEmpty() || p <= previousPosition) {
            cycles.add(mutableListOf())
        }
        cycles.last().add(item)
        previousPosition = p
    }

    return cycles.mapIndexed { index, group ->
        CompletedCycle(
            cycleIndex = index,
            completedDays = group.mapNotNull { position(it) }.distinct().size,
            totalDays = dayCount,
            workouts = group,
        )
    }
}

// Implements the cycle position algorithm from 01-data-model.md.
fun computeCycle(dayCount: Int, workouts: List<WorkoutForCycle>): CycleResult {
    require(dayCount >= 1) { "dayCount must be at least 1, was $dayCount" }
    workouts.forEach { w ->
        w.position?.let {
            require(it in 0 until dayCount) { "position $it is out of range for dayCount $dayCount" }
        }
    }

    val ordered = workouts
        .filter { it.position != null }
        .sortedWith(compareBy({ it.date }, { it.startedAt }))

    var cycleIndex = 0
    var previousPosition = -1
    val currentCycleDone = mutableMapOf<Int, LocalDate>()

    for (w in ordered) {
        val p = w.position!!
        if (p <= previousPosition) {
            cycleIndex += 1
            currentCycleDone.clear()
        }
        currentCycleDone[p] = w.date
        previousPosition = p
    }

    var lastPosition = previousPosition
    var nextPosition = lastPosition + 1
    if (nextPosition >= dayCount) {
        nextPosition = 0
        currentCycleDone.clear()
        cycleIndex += 1
        // The cycle that just completed is gone; nothing should read as skipped in the
        // fresh one until the user actually misses a day in it.
        lastPosition = -1
    }

    val days = (0 until dayCount).map { position ->
        val status = when {
            currentCycleDone.containsKey(position) -> DayCycleStatus.DONE
            position == nextPosition -> DayCycleStatus.NEXT
            position < lastPosition -> DayCycleStatus.SKIPPED
            else -> DayCycleStatus.UPCOMING
        }
        CycleDayState(position = position, status = status, workoutDate = currentCycleDone[position])
    }

    return CycleResult(cycleIndex = cycleIndex, nextPosition = nextPosition, days = days)
}
