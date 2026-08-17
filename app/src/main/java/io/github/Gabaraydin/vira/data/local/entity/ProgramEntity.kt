package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "program")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Exactly one row true, enforced by the repository, not the database.
    val isActive: Boolean,
    val createdAt: Long,
    val archivedAt: Long?,
)
