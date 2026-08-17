package io.github.Gabaraydin.vira.domain.cycle

import java.time.LocalDate

// position == null covers both ad-hoc workouts (no program day) and workouts whose
// program day was later deleted — the caller resolves programDayId to a position and
// passes null when it can't, so this function excludes both the same way.
data class WorkoutForCycle(val position: Int?, val date: LocalDate, val startedAt: Long)

enum class DayCycleStatus { DONE, NEXT, SKIPPED, UPCOMING }

data class CycleDayState(val position: Int, val status: DayCycleStatus, val workoutDate: LocalDate?)

data class CycleResult(val cycleIndex: Int, val nextPosition: Int, val days: List<CycleDayState>)

// Implements the cycle position algorithm from 01-data-model.md literally, including that
// lastPosition is NOT reset when a cycle just completed (nextPosition wraps to 0) — only
// nextPosition and the done-set reset. That leaves positions between the wrapped next (0)
// and the old lastPosition marked skipped rather than upcoming until the user logs again.
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

    val lastPosition = previousPosition
    var nextPosition = lastPosition + 1
    if (nextPosition >= dayCount) {
        nextPosition = 0
        currentCycleDone.clear()
        cycleIndex += 1
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
