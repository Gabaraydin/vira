package io.github.Gabaraydin.vira.domain.calculations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VolumeTest {

    @Test
    fun `empty list has zero volume`() {
        assertEquals(0.0, totalVolume(emptyList()), 0.0)
    }

    @Test
    fun `sums weight times reps over completed non-warmup sets`() {
        val sets = listOf(
            SetForVolume(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true),
            SetForVolume(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true),
        )
        assertEquals(1000.0, totalVolume(sets), 0.0)
    }

    @Test
    fun `excludes warmup sets`() {
        val sets = listOf(
            SetForVolume(weightKg = 40.0, reps = 10, isWarmup = true, isCompleted = true),
            SetForVolume(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true),
        )
        assertEquals(500.0, totalVolume(sets), 0.0)
    }

    @Test
    fun `excludes uncompleted sets`() {
        val sets = listOf(
            SetForVolume(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = false),
            SetForVolume(weightKg = 100.0, reps = 5, isWarmup = false, isCompleted = true),
        )
        assertEquals(500.0, totalVolume(sets), 0.0)
    }

    @Test
    fun `a set that is both warmup and uncompleted is still excluded once`() {
        val sets = listOf(
            SetForVolume(weightKg = 40.0, reps = 10, isWarmup = true, isCompleted = false),
        )
        assertEquals(0.0, totalVolume(sets), 0.0)
    }

    @Test
    fun `negative weight is rejected`() {
        val sets = listOf(SetForVolume(weightKg = -1.0, reps = 5, isWarmup = false, isCompleted = true))
        assertThrows(IllegalArgumentException::class.java) { totalVolume(sets) }
    }

    @Test
    fun `negative reps is rejected`() {
        val sets = listOf(SetForVolume(weightKg = 10.0, reps = -1, isWarmup = false, isCompleted = true))
        assertThrows(IllegalArgumentException::class.java) { totalVolume(sets) }
    }

    @Test
    fun `zero weight or reps are valid and contribute nothing`() {
        val sets = listOf(SetForVolume(weightKg = 0.0, reps = 0, isWarmup = false, isCompleted = true))
        assertEquals(0.0, totalVolume(sets), 0.0)
    }
}
