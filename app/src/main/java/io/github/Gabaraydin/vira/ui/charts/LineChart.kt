package io.github.Gabaraydin.vira.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.Gabaraydin.vira.domain.charts.ChartPoint
import io.github.Gabaraydin.vira.domain.charts.computeChartBounds
import io.github.Gabaraydin.vira.domain.charts.computeYAxisTicks

// A minimal, reusable line chart: one series, a left-hand axis of evenly spaced tick
// labels, and an empty state when there's nothing to plot. Shared by Exercise Detail's
// e1RM chart and the Body screen's weight/body-fat charts — deliberately generic (plain
// x/y doubles, caller-supplied label formatting) rather than knowing about exercises or
// body measurements itself.
@Composable
fun LineChart(
    points: List<ChartPoint>,
    emptyStateText: String,
    modifier: Modifier = Modifier,
    yAxisFormatter: (Double) -> String = { it.toInt().toString() },
) {
    val bounds = remember(points) { computeChartBounds(points) }

    if (bounds == null) {
        Box(modifier = modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text(emptyStateText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    val ticks = computeYAxisTicks(bounds, tickCount = 4)
    val lineColor = MaterialTheme.colorScheme.primary
    val sortedPoints = remember(points) { points.sortedBy { it.x } }

    Row(modifier = modifier.fillMaxWidth().height(160.dp)) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight().padding(end = 4.dp),
        ) {
            ticks.reversed().forEach { tick -> Text(yAxisFormatter(tick), style = MaterialTheme.typography.labelSmall) }
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val xSpan = (bounds.maxX - bounds.minX).takeIf { it != 0.0 } ?: 1.0
            val ySpan = (bounds.maxY - bounds.minY).takeIf { it != 0.0 } ?: 1.0
            fun mapX(x: Double) = ((x - bounds.minX) / xSpan * size.width).toFloat()
            fun mapY(y: Double) = (size.height - (y - bounds.minY) / ySpan * size.height).toFloat()

            val path = Path()
            sortedPoints.forEachIndexed { index, point ->
                val offset = Offset(mapX(point.x), mapY(point.y))
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 4f))
            sortedPoints.forEach { point -> drawCircle(color = lineColor, radius = 6f, center = Offset(mapX(point.x), mapY(point.y))) }
        }
    }
}
