package io.github.Gabaraydin.vira.domain.charts

data class ChartPoint(val x: Double, val y: Double)

data class ChartBounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)

// Null for an empty series — the caller renders an empty state instead of a chart. A
// single point (or every point sharing one X or Y) would otherwise divide by a zero
// range when mapping into screen space, so that axis gets a synthetic +/-1 span.
fun computeChartBounds(points: List<ChartPoint>): ChartBounds? {
    if (points.isEmpty()) return null

    val xs = points.map { it.x }
    val ys = points.map { it.y }
    val minX = xs.min()
    val maxX = xs.max()
    val minY = ys.min()
    val maxY = ys.max()

    return ChartBounds(
        minX = if (minX == maxX) minX - 1 else minX,
        maxX = if (minX == maxX) maxX + 1 else maxX,
        minY = if (minY == maxY) minY - 1 else minY,
        maxY = if (minY == maxY) maxY + 1 else maxY,
    )
}

// Evenly spaced Y-axis tick values across the bounds, including both ends.
fun computeYAxisTicks(bounds: ChartBounds, tickCount: Int): List<Double> {
    require(tickCount >= 2) { "tickCount must be at least 2, was $tickCount" }
    val step = (bounds.maxY - bounds.minY) / (tickCount - 1)
    return (0 until tickCount).map { bounds.minY + step * it }
}
