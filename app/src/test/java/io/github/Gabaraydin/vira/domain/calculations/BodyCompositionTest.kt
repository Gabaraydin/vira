package io.github.Gabaraydin.vira.domain.calculations

import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BodyCompositionTest {

    @Test
    fun `male body fat matches the US Navy formula for a typical measurement`() {
        val bf = bodyFatPercentMale(waistCm = 85.0, neckCm = 38.0, heightCm = 180.0)
        assertEquals(16.1066, bf, 0.001)
    }

    @Test
    fun `male formula rejects waist not greater than neck`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyFatPercentMale(waistCm = 38.0, neckCm = 38.0, heightCm = 180.0)
        }
    }

    @Test
    fun `male formula rejects a result outside the plausible range`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyFatPercentMale(waistCm = 200.0, neckCm = 30.0, heightCm = 140.0)
        }
    }

    @Test
    fun `male formula rejects non-positive height`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyFatPercentMale(waistCm = 85.0, neckCm = 38.0, heightCm = 0.0)
        }
    }

    @Test
    fun `female body fat matches the US Navy formula for a typical measurement`() {
        val bf = bodyFatPercentFemale(waistCm = 75.0, neckCm = 32.0, hipCm = 95.0, heightCm = 165.0)
        assertEquals(27.4256, bf, 0.001)
    }

    @Test
    fun `female formula rejects non-positive hip`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyFatPercentFemale(waistCm = 75.0, neckCm = 32.0, hipCm = 0.0, heightCm = 165.0)
        }
    }

    @Test
    fun `female formula rejects a non-positive waist plus hip minus neck`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyFatPercentFemale(waistCm = 10.0, neckCm = 30.0, hipCm = 10.0, heightCm = 165.0)
        }
    }

    @Test
    fun `lean mass subtracts the fat fraction from total weight`() {
        assertEquals(64.0, leanMassKg(weightKg = 80.0, bodyFatPercent = 20.0), 0.0001)
    }

    @Test
    fun `lean mass rejects a body fat percent outside the plausible range`() {
        assertThrows(IllegalArgumentException::class.java) {
            leanMassKg(weightKg = 80.0, bodyFatPercent = 0.5)
        }
    }

    @Test
    fun `fat mass is total weight minus lean mass`() {
        assertEquals(16.0, fatMassKg(weightKg = 80.0, leanMassKg = 64.0), 0.0001)
    }

    @Test
    fun `fat mass rejects a lean mass greater than total weight`() {
        assertThrows(IllegalArgumentException::class.java) {
            fatMassKg(weightKg = 80.0, leanMassKg = 90.0)
        }
    }

    @Test
    fun `bmi divides weight by height in metres squared`() {
        assertEquals(24.6914, bodyMassIndex(weightKg = 80.0, heightCm = 180.0), 0.001)
    }

    @Test
    fun `bmi rejects non-positive weight`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyMassIndex(weightKg = 0.0, heightCm = 180.0)
        }
    }

    @Test
    fun `bmi rejects non-positive height`() {
        assertThrows(IllegalArgumentException::class.java) {
            bodyMassIndex(weightKg = 80.0, heightCm = 0.0)
        }
    }

    // --- bodyFatCategory ---

    @Test
    fun `male category bands`() {
        assertEquals(BodyFatCategory.ESSENTIAL_FAT, bodyFatCategory(3.0, BiologicalSex.MALE))
        assertEquals(BodyFatCategory.ATHLETES, bodyFatCategory(10.0, BiologicalSex.MALE))
        assertEquals(BodyFatCategory.FITNESS, bodyFatCategory(15.0, BiologicalSex.MALE))
        assertEquals(BodyFatCategory.ACCEPTABLE, bodyFatCategory(20.0, BiologicalSex.MALE))
        assertEquals(BodyFatCategory.OBESE, bodyFatCategory(30.0, BiologicalSex.MALE))
    }

    @Test
    fun `female category bands`() {
        assertEquals(BodyFatCategory.ESSENTIAL_FAT, bodyFatCategory(11.0, BiologicalSex.FEMALE))
        assertEquals(BodyFatCategory.ATHLETES, bodyFatCategory(16.0, BiologicalSex.FEMALE))
        assertEquals(BodyFatCategory.FITNESS, bodyFatCategory(22.0, BiologicalSex.FEMALE))
        assertEquals(BodyFatCategory.ACCEPTABLE, bodyFatCategory(28.0, BiologicalSex.FEMALE))
        assertEquals(BodyFatCategory.OBESE, bodyFatCategory(35.0, BiologicalSex.FEMALE))
    }

    @Test
    fun `category band boundary is inclusive on the lower band`() {
        assertEquals(BodyFatCategory.ESSENTIAL_FAT, bodyFatCategory(5.0, BiologicalSex.MALE))
        assertEquals(BodyFatCategory.ATHLETES, bodyFatCategory(5.0001, BiologicalSex.MALE))
    }
}
