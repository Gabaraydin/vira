package io.github.Gabaraydin.vira.domain.calculations

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
}
