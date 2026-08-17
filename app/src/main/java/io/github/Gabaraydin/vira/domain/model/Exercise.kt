package io.github.Gabaraydin.vira.domain.model

data class Exercise(
    val id: Long,
    val nameEn: String,
    val nameTr: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
    val defaultRestSec: Int?,
    val isCustom: Boolean,
    val isArchived: Boolean,
    val notes: String?,
)
