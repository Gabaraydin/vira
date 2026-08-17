package io.github.Gabaraydin.vira.domain.seed

import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup

data class SeedExercise(
    val nameEn: String,
    val nameTr: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
)
