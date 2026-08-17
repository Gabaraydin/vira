package io.github.Gabaraydin.vira.domain.model

data class ProgramDayExercise(
    val id: Long,
    val programDayId: Long,
    val exerciseId: Long,
    val position: Int,
    val supersetGroupId: Int?,
    val supersetOrder: Int?,
    val targetSets: Int,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetWeightKg: Double?,
    val restSecOverride: Int?,
)
