package io.github.Gabaraydin.vira.domain.model

import java.util.Locale

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

// Follows the system locale; the app doesn't force a language of its own until #21.
fun Exercise.displayName(): String = if (Locale.getDefault().language == "tr") nameTr else nameEn
