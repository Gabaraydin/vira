package io.github.Gabaraydin.vira.domain.calculations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordsTest {

    @Test
    fun `beating prior best e1RM is a new PR`() {
        val new = listOf(SetForPr(weightKg = 110.0, reps = 5, isWarmup = false, isCompleted = true))
        val prior = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        assertTrue(isNewPersonalRecord(new, prior))
    }

    @Test
    fun `tying prior best is not a new PR`() {
        val new = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        val prior = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        assertFalse(isNewPersonalRecord(new, prior))
    }

    @Test
    fun `falling short of prior best is not a new PR`() {
        val new = listOf(SetForPr(weightKg = 90.0, reps = 5, isWarmup = false, isCompleted = true))
        val prior = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        assertFalse(isNewPersonalRecord(new, prior))
    }

    @Test
    fun `no prior history is not a new PR`() {
        val new = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        assertFalse(isNewPersonalRecord(new, emptyList()))
    }

    @Test
    fun `warmup sets are ignored on both sides`() {
        val new = listOf(SetForPr(weightKg = 200.0, reps = 5, isWarmup = true, isCompleted = true))
        val prior = listOf(SetForPr(weightKg = 50.0, reps = 5, isWarmup = false, isCompleted = true))
        assertFalse(isNewPersonalRecord(new, prior))
    }

    @Test
    fun `incomplete sets are ignored on both sides`() {
        val new = listOf(SetForPr(weightKg = 200.0, reps = 5, isWarmup = false, isCompleted = false))
        val prior = listOf(SetForPr(weightKg = 50.0, reps = 5, isWarmup = false, isCompleted = true))
        assertFalse(isNewPersonalRecord(new, prior))
    }

    @Test
    fun `an unfilled 0kg set does not crash and does not count as a PR`() {
        val new = listOf(SetForPr(weightKg = 0.0, reps = 0, isWarmup = false, isCompleted = true))
        val prior = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        assertFalse(isNewPersonalRecord(new, prior))
    }

    @Test
    fun `best set among several is used, not the first`() {
        val new = listOf(
            SetForPr(weightKg = 80.0, reps = 5, isWarmup = false, isCompleted = true),
            SetForPr(weightKg = 120.0, reps = 3, isWarmup = false, isCompleted = true),
        )
        val prior = listOf(SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true))
        assertTrue(isNewPersonalRecord(new, prior))
    }

    // --- bestWeightPerRepCount ---

    @Test
    fun `best weight per rep count picks the heaviest set for each rep count`() {
        val sets = listOf(
            SetForPr(weightKg = 80.0, reps = 5, isWarmup = false, isCompleted = true),
            SetForPr(weightKg = 90.0, reps = 5, isWarmup = false, isCompleted = true),
            SetForPr(weightKg = 100.0, reps = 1, isWarmup = false, isCompleted = true),
        )
        val table = bestWeightPerRepCount(sets)

        assertEquals(listOf(RepPr(1, 100.0), RepPr(5, 90.0)), table)
    }

    @Test
    fun `best weight per rep count excludes warmup, incomplete, and unfilled sets`() {
        val sets = listOf(
            SetForPr(weightKg = 200.0, reps = 5, isWarmup = true, isCompleted = true),
            SetForPr(weightKg = 200.0, reps = 5, isWarmup = false, isCompleted = false),
            SetForPr(weightKg = 0.0, reps = 0, isWarmup = false, isCompleted = true),
        )
        assertEquals(emptyList<RepPr>(), bestWeightPerRepCount(sets))
    }

    @Test
    fun `best weight per rep count on an empty list is empty`() {
        assertEquals(emptyList<RepPr>(), bestWeightPerRepCount(emptyList()))
    }

    // --- bestOverallEstimatedOneRepMax ---

    @Test
    fun `best overall e1RM picks the highest estimate across sets`() {
        val sets = listOf(
            SetForPr(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true),
            SetForPr(weightKg = 120.0, reps = 1, isWarmup = false, isCompleted = true),
        )
        // Epley: 100*(1+5/30)=116.67, 120*(1+1/30)=124.0 -> the second wins.
        assertEquals(124.0, bestOverallEstimatedOneRepMax(sets)!!, 0.01)
    }

    @Test
    fun `best overall e1RM is null with no qualifying sets`() {
        assertEquals(null, bestOverallEstimatedOneRepMax(emptyList()))
    }
}
