package io.github.Gabaraydin.vira.domain.calculations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WeightConversionTest {

    @Test
    fun `100 kg converts to about 220,46 lb`() {
        assertEquals(220.46226, 100.0.kgToLb(), 0.0001)
    }

    @Test
    fun `100 lb converts to about 45,36 kg`() {
        assertEquals(45.359237, 100.0.lbToKg(), 0.0001)
    }

    @Test
    fun `zero kg is zero lb`() {
        assertEquals(0.0, 0.0.kgToLb(), 0.0)
    }

    @Test
    fun `zero lb is zero kg`() {
        assertEquals(0.0, 0.0.lbToKg(), 0.0)
    }

    @Test
    fun `round trip kg to lb and back returns the original value`() {
        val original = 73.5
        assertEquals(original, original.kgToLb().lbToKg(), 0.0000001)
    }

    @Test
    fun `negative kg is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { (-1.0).kgToLb() }
    }

    @Test
    fun `negative lb is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { (-1.0).lbToKg() }
    }
}
