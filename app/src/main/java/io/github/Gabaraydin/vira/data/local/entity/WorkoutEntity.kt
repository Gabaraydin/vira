package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "workout",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["date"]), Index(value = ["programDayId"])],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // null for ad-hoc sessions, or once the program day it was logged against is deleted.
    val programDayId: Long?,
    // Copied at creation time so renaming/reordering/deleting a program day never
    // corrupts history — always render historical workouts from this, not the live day.
    val dayNameSnapshot: String,
    val programNameSnapshot: String?,
    val date: LocalDate,
    val startedAt: Long,
    // null while the session is active; the repository enforces at most one such row.
    val finishedAt: Long?,
    val note: String?,
)
