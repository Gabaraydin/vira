package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.Gabaraydin.vira.domain.model.MuscleGroup

@Entity(
    tableName = "program_day",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["programId", "position"], unique = true)],
)
data class ProgramDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    // 0-based, contiguous, defines cycle order. Contiguity is enforced by the repository.
    val position: Int,
    val name: String,
    val isRest: Boolean,
    // Opens the preset picker on the right list; unset for a manually built day.
    val libraryCategory: MuscleGroup?,
)
