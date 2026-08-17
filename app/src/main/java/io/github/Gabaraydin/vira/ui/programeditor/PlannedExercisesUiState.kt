package io.github.Gabaraydin.vira.ui.programeditor

data class PlannedExerciseUiModel(
    val entryId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Int,
    val targetSets: Int,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val supersetGroupId: Int?,
    val supersetOrder: Int?,
    val supersetLabel: String?,
)

data class PlannedExercisesUiState(val dayName: String = "", val exercises: List<PlannedExerciseUiModel> = emptyList())
