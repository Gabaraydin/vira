package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_day_exercise",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    // Not called out in the spec, but Room flags unindexed FK columns as a full-table-scan
    // risk on every parent update/delete, so both get one.
    indices = [Index(value = ["programDayId"]), Index(value = ["exerciseId"])],
)
data class ProgramDayExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDayId: Long,
    val exerciseId: Long,
    val position: Int,
    // null = standalone; equal values across rows = same superset group (2-4 members,
    // contiguous by position — enforced by the repository, not the database).
    val supersetGroupId: Int?,
    val supersetOrder: Int?,
    val targetSets: Int,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetWeightKg: Double?,
    val restSecOverride: Int?,
)
