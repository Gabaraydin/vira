package io.github.Gabaraydin.vira.ui.exercisepicker

import io.github.Gabaraydin.vira.domain.model.Exercise

data class ExercisePickerUiState(
    val query: String = "",
    val exercises: List<Exercise> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
)
