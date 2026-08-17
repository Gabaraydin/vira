package io.github.Gabaraydin.vira.domain.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ChartMathTest {

    @Test
    fun `empty series has no bounds`() {
        assertNull(computeChartBounds(emptyList()))
    }

    @Test
    fun `bounds span the min and max of the series`() {
        val points = listOf(ChartPoint(0.0, 10.0), ChartPoint(1.0, 30.0), ChartPoint(2.0, 20.0))
        val bounds = computeChartBounds(points)

        assertEquals(0.0, bounds!!.minX, 0.0)
        assertEquals(2.0, bounds.maxX, 0.0)
        assertEquals(10.0, bounds.minY, 0.0)
        assertEquals(30.0, bounds.maxY, 0.0)
    }

    @Test
    fun `a single point gets a synthetic non-zero range on both axes`() {
        val bounds = computeChartBounds(listOf(ChartPoint(5.0, 5.0)))

        assertEquals(4.0, bounds!!.minX, 0.0)
        assertEquals(6.0, bounds.maxX, 0.0)
        assertEquals(4.0, bounds.minY, 0.0)
        assertEquals(6.0, bounds.maxY, 0.0)
    }

    @Test
    fun `every point sharing the same Y still gets a synthetic Y range`() {
        val points = listOf(ChartPoint(0.0, 100.0), ChartPoint(1.0, 100.0))
        val bounds = computeChartBounds(points)

        assertEquals(99.0, bounds!!.minY, 0.0)
        assertEquals(101.0, bounds.maxY, 0.0)
        // X still varies here, so it's untouched.
        assertEquals(0.0, bounds.minX, 0.0)
        assertEquals(1.0, bounds.maxX, 0.0)
    }

    @Test
    fun `y axis ticks are evenly spaced and include both ends`() {
        val bounds = ChartBounds(minX = 0.0, maxX = 1.0, minY = 0.0, maxY = 30.0)
        val ticks = computeYAxisTicks(bounds, tickCount = 4)

        assertEquals(listOf(0.0, 10.0, 20.0, 30.0), ticks)
    }

    @Test
    fun `two ticks is just the two bounds`() {
        val bounds = ChartBounds(minX = 0.0, maxX = 1.0, minY = 5.0, maxY = 15.0)
        assertEquals(listOf(5.0, 15.0), computeYAxisTicks(bounds, tickCount = 2))
    }

    @Test
    fun `fewer than two ticks is rejected`() {
        val bounds = ChartBounds(minX = 0.0, maxX = 1.0, minY = 0.0, maxY = 1.0)
        assertThrows(IllegalArgumentException::class.java) { computeYAxisTicks(bounds, tickCount = 1) }
    }
}
