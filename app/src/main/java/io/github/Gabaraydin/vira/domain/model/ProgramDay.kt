package io.github.Gabaraydin.vira.domain.model

data class ProgramDay(
    val id: Long,
    val programId: Long,
    val position: Int,
    val name: String,
    val isRest: Boolean,
    val libraryCategory: MuscleGroup?,
)
