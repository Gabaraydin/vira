package io.github.Gabaraydin.vira.domain.calculations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OneRepMaxTest {

    @Test
    fun `epley formula matches spec for a typical set`() {
        val result = estimatedOneRepMax(weightKg = 100.0, reps = 5)
        assertEquals(116.666666, result.kg, 0.0001)
    }

    @Test
    fun `single rep set returns the weight itself scaled by the formula`() {
        val result = estimatedOneRepMax(weightKg = 80.0, reps = 1)
        assertEquals(80.0 * (1 + 1 / 30.0), result.kg, 0.0001)
    }

    @Test
    fun `reps of exactly 12 is not approximate`() {
        val result = estimatedOneRepMax(weightKg = 60.0, reps = 12)
        assertFalse(result.isApproximate)
    }

    @Test
    fun `reps above 12 is flagged approximate`() {
        val result = estimatedOneRepMax(weightKg = 60.0, reps = 13)
        assertTrue(result.isApproximate)
    }

    @Test
    fun `zero weight is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            estimatedOneRepMax(weightKg = 0.0, reps = 5)
        }
    }

    @Test
    fun `negative weight is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            estimatedOneRepMax(weightKg = -10.0, reps = 5)
        }
    }

    @Test
    fun `zero reps is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            estimatedOneRepMax(weightKg = 100.0, reps = 0)
        }
    }

    @Test
    fun `negative reps is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            estimatedOneRepMax(weightKg = 100.0, reps = -1)
        }
    }
}
