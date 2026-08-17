package io.github.Gabaraydin.vira.domain.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

private val DAY1: LocalDate = LocalDate.of(2026, 1, 1)
private val DAY2: LocalDate = LocalDate.of(2026, 1, 2)

class CycleEngineTest {

    @Test
    fun `no workouts at all means next is day zero and cycle is the first one`() {
        val result = computeCycle(dayCount = 3, workouts = emptyList())

        assertEquals(0, result.cycleIndex)
        assertEquals(0, result.nextPosition)
        assertEquals(DayCycleStatus.NEXT, result.days[0].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[1].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[2].status)
    }

    @Test
    fun `last workout on the final day empties the strip and points next at day zero`() {
        val workouts = listOf(WorkoutForCycle(position = 2, date = DAY1, startedAt = 100))
        val result = computeCycle(dayCount = 3, workouts = workouts)

        assertEquals(1, result.cycleIndex)
        assertEquals(0, result.nextPosition)
        // Nothing is DONE or SKIPPED: the completed cycle is fully cleared, fresh start.
        assertEquals(DayCycleStatus.NEXT, result.days[0].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[1].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[2].status)
    }

    @Test
    fun `two workouts on the same date both count`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = DAY1, startedAt = 100),
            WorkoutForCycle(position = 1, date = DAY1, startedAt = 200),
        )
        val result = computeCycle(dayCount = 3, workouts = workouts)

        assertEquals(0, result.cycleIndex)
        assertEquals(2, result.nextPosition)
        assertEquals(DayCycleStatus.DONE, result.days[0].status)
        assertEquals(DayCycleStatus.DONE, result.days[1].status)
        assertEquals(DayCycleStatus.NEXT, result.days[2].status)
    }

    @Test
    fun `same-date workouts are ordered by startedAt regardless of input order`() {
        val workouts = listOf(
            // Given out of order: position 1 first in the list, but its startedAt is later.
            WorkoutForCycle(position = 1, date = DAY1, startedAt = 200),
            WorkoutForCycle(position = 0, date = DAY1, startedAt = 100),
        )
        val result = computeCycle(dayCount = 3, workouts = workouts)

        // If startedAt weren't honoured, position 1 would be processed first and the
        // p <= previousPosition wrap check would fire early, breaking the cycle count.
        assertEquals(0, result.cycleIndex)
        assertEquals(2, result.nextPosition)
        assertEquals(DayCycleStatus.DONE, result.days[0].status)
        assertEquals(DayCycleStatus.DONE, result.days[1].status)
    }

    @Test
    fun `an ad-hoc workout with no position is excluded entirely`() {
        val workouts = listOf(WorkoutForCycle(position = null, date = DAY1, startedAt = 100))
        val result = computeCycle(dayCount = 3, workouts = workouts)

        assertEquals(0, result.cycleIndex)
        assertEquals(0, result.nextPosition)
        assertEquals(DayCycleStatus.NEXT, result.days[0].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[1].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[2].status)
    }

    @Test
    fun `a workout whose program day was deleted is excluded the same way as ad-hoc`() {
        // The repository resolves programDayId to a position; a deleted day resolves to
        // null just like an ad-hoc workout does, so the domain function treats them alike.
        val workouts = listOf(WorkoutForCycle(position = null, date = DAY1, startedAt = 100))
        val result = computeCycle(dayCount = 3, workouts = workouts)

        assertEquals(0, result.nextPosition)
        assertEquals(DayCycleStatus.NEXT, result.days[0].status)
    }

    @Test
    fun `completing a full cycle advances to a fresh cycle with day zero as next`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = DAY1, startedAt = 100),
            WorkoutForCycle(position = 1, date = DAY2, startedAt = 100),
        )
        val result = computeCycle(dayCount = 2, workouts = workouts)

        assertEquals(1, result.cycleIndex)
        assertEquals(0, result.nextPosition)
        assertEquals(DayCycleStatus.NEXT, result.days[0].status)
        assertEquals(DayCycleStatus.UPCOMING, result.days[1].status)
    }

    @Test
    fun `a done day carries the date it was logged on`() {
        val workouts = listOf(WorkoutForCycle(position = 0, date = DAY1, startedAt = 100))
        val result = computeCycle(dayCount = 3, workouts = workouts)

        assertEquals(DAY1, result.days[0].workoutDate)
        assertNull(result.days[1].workoutDate)
    }

    @Test
    fun `two full cycles then a partial third tracks cycleIndex correctly`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 1), startedAt = 1),
            WorkoutForCycle(position = 1, date = LocalDate.of(2026, 1, 2), startedAt = 1),
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 8), startedAt = 1),
            WorkoutForCycle(position = 1, date = LocalDate.of(2026, 1, 9), startedAt = 1),
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 15), startedAt = 1),
        )
        val result = computeCycle(dayCount = 2, workouts = workouts)

        assertEquals(2, result.cycleIndex)
        assertEquals(1, result.nextPosition)
        assertEquals(DayCycleStatus.DONE, result.days[0].status)
        assertEquals(DayCycleStatus.NEXT, result.days[1].status)
    }

    @Test
    fun `dayCount below one is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            computeCycle(dayCount = 0, workouts = emptyList())
        }
    }

    @Test
    fun `a workout position outside the program's range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            computeCycle(dayCount = 2, workouts = listOf(WorkoutForCycle(position = 5, date = DAY1, startedAt = 1)))
        }
    }

    // --- groupCompletedCycles ---

    private fun group(dayCount: Int, workouts: List<WorkoutForCycle>) = groupCompletedCycles(
        dayCount = dayCount,
        items = workouts,
        position = { it.position },
        date = { it.date },
        startedAt = { it.startedAt },
    )

    @Test
    fun `no workouts groups into no cycles`() {
        assertEquals(emptyList<Any>(), group(dayCount = 3, workouts = emptyList()))
    }

    @Test
    fun `a single full cycle is one group with completed equal to total`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = DAY1, startedAt = 1),
            WorkoutForCycle(position = 1, date = DAY2, startedAt = 1),
        )
        val cycles = group(dayCount = 2, workouts = workouts)

        assertEquals(1, cycles.size)
        assertEquals(0, cycles[0].cycleIndex)
        assertEquals(2, cycles[0].completedDays)
        assertEquals(2, cycles[0].totalDays)
    }

    @Test
    fun `two cycles are split at the position wrap, oldest first`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 1), startedAt = 1),
            WorkoutForCycle(position = 1, date = LocalDate.of(2026, 1, 2), startedAt = 1),
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 8), startedAt = 1),
        )
        val cycles = group(dayCount = 2, workouts = workouts)

        assertEquals(2, cycles.size)
        assertEquals(0, cycles[0].cycleIndex)
        assertEquals(2, cycles[0].completedDays)
        assertEquals(1, cycles[1].cycleIndex)
        assertEquals(1, cycles[1].completedDays)
    }

    @Test
    fun `a skipped day still leaves the cycle group with fewer completed than total`() {
        val workouts = listOf(WorkoutForCycle(position = 0, date = DAY1, startedAt = 1))
        val cycles = group(dayCount = 3, workouts = workouts)

        assertEquals(1, cycles.size)
        assertEquals(1, cycles[0].completedDays)
        assertEquals(3, cycles[0].totalDays)
    }

    @Test
    fun `repeating the same position within a cycle only counts once`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 1), startedAt = 1),
            WorkoutForCycle(position = 0, date = LocalDate.of(2026, 1, 2), startedAt = 1),
        )
        val cycles = group(dayCount = 3, workouts = workouts)

        // Same position twice in a row means the second one starts a new cycle (p <=
        // previousPosition), so this is two one-day cycles, not one two-visit cycle.
        assertEquals(2, cycles.size)
        assertEquals(1, cycles[0].completedDays)
        assertEquals(1, cycles[1].completedDays)
    }

    @Test
    fun `ad-hoc workouts with no position are excluded from cycle grouping`() {
        val workouts = listOf(
            WorkoutForCycle(position = 0, date = DAY1, startedAt = 1),
            WorkoutForCycle(position = null, date = DAY2, startedAt = 1),
        )
        val cycles = group(dayCount = 2, workouts = workouts)

        assertEquals(1, cycles.size)
        assertEquals(1, cycles[0].completedDays)
        assertEquals(1, cycles[0].workouts.size)
    }
}
