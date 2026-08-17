package io.github.Gabaraydin.vira.domain.model

data class Program(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val createdAt: Long,
    val archivedAt: Long?,
)
