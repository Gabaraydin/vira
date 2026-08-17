package io.github.Gabaraydin.vira.ui.body

import io.github.Gabaraydin.vira.domain.calculations.BodyFatCategory
import io.github.Gabaraydin.vira.domain.charts.ChartPoint
import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import java.time.LocalDate

data class BodyResultUiModel(
    val bodyFatPercent: Double,
    val category: BodyFatCategory,
    val leanMassKg: Double,
    val fatMassKg: Double,
    val bmi: Double,
)

data class BodyHistoryRow(val id: Long, val date: LocalDate, val weightKg: Double, val bodyFatPct: Double?)

data class BodyUiState(
    val isLoading: Boolean = true,
    val sex: BiologicalSex = BiologicalSex.MALE,
    val latestResult: BodyResultUiModel? = null,
    val weightChartPoints: List<ChartPoint> = emptyList(),
    val bodyFatChartPoints: List<ChartPoint> = emptyList(),
    val history: List<BodyHistoryRow> = emptyList(),
)
