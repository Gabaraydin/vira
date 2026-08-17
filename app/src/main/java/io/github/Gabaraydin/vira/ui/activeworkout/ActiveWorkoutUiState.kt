package io.github.Gabaraydin.vira.ui.activeworkout

data class PreviousSetSummary(val weightKg: Double, val reps: Int, val isWarmup: Boolean)

data class ActiveSetUiModel(
    val id: Long,
    val exerciseId: Long,
    val position: Int,
    val setIndex: Int,
    val supersetGroupId: Int?,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val completedAt: Long?,
)

data class ActiveExerciseUiModel(
    val exerciseId: Long,
    val exerciseName: String,
    val supersetLabel: String?,
    val previousSets: List<PreviousSetSummary>,
    val sets: List<ActiveSetUiModel>,
)

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val dayName: String = "",
    val startedAt: Long = 0,
    val rpeEnabled: Boolean = false,
    val keepScreenOn: Boolean = true,
    val exercises: List<ActiveExerciseUiModel> = emptyList(),
) {
    val hasIncompleteSets: Boolean get() = exercises.any { ex -> ex.sets.any { !it.isCompleted } }
}
