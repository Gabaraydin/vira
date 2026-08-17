package io.github.Gabaraydin.vira.ui.exercisedetail

import io.github.Gabaraydin.vira.domain.calculations.RepPr
import io.github.Gabaraydin.vira.domain.charts.ChartPoint
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import java.time.LocalDate

data class SessionSetUiModel(val weightKg: Double, val reps: Int)

data class SessionHistoryRow(val date: LocalDate, val sets: List<SessionSetUiModel>)

data class ExerciseDetailUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val muscle: MuscleGroup = MuscleGroup.CHEST,
    val equipment: Equipment = Equipment.BARBELL,
    val chartPoints: List<ChartPoint> = emptyList(),
    val bestOverallE1rm: Double? = null,
    val repPrTable: List<RepPr> = emptyList(),
    val history: List<SessionHistoryRow> = emptyList(),
    val restOverrideSeconds: Int? = null,
    val isArchived: Boolean = false,
)
