package io.github.Gabaraydin.vira.ui.workoutsummary

data class PreviousCycleComparison(
    val previousVolumeKg: Double,
    val previousDurationSec: Long,
    val previousSetCount: Int,
)

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val dayName: String = "",
    val durationSec: Long = 0,
    val totalVolumeKg: Double = 0.0,
    val setCount: Int = 0,
    val newPrExerciseNames: List<String> = emptyList(),
    val comparison: PreviousCycleComparison? = null,
)
