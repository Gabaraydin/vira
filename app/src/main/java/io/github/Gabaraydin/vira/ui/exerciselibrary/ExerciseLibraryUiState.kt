package io.github.Gabaraydin.vira.ui.exerciselibrary

import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import java.time.LocalDate

data class ExerciseRowUiModel(
    val id: Long,
    val name: String,
    val muscle: MuscleGroup,
    val equipment: Equipment,
    val lastPerformed: LocalDate?,
)

data class ExerciseLibraryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val muscleFilter: MuscleGroup? = null,
    val equipmentFilter: Equipment? = null,
    val customOnly: Boolean = false,
    val exercises: List<ExerciseRowUiModel> = emptyList(),
)
