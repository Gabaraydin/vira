package io.github.Gabaraydin.vira.ui.workoutdetail

data class WorkoutDetailSetUiModel(
    val id: Long,
    val exerciseId: Long,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val isWarmup: Boolean,
)

data class WorkoutDetailExerciseUiModel(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<WorkoutDetailSetUiModel>,
)

data class WorkoutDetailUiState(
    val isLoading: Boolean = true,
    val dayName: String = "",
    val date: String = "",
    val durationSec: Long = 0,
    val totalVolumeKg: Double = 0.0,
    val note: String = "",
    val exercises: List<WorkoutDetailExerciseUiModel> = emptyList(),
)
