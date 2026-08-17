package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup

@Entity(
    tableName = "exercise",
    indices = [Index(value = ["isArchived", "primaryMuscle"])],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameTr: String,
    val primaryMuscle: MuscleGroup,
    // Comma-separated MuscleGroup names; may be empty. Kept as a plain string rather than
    // a converter-mapped list because it's only ever displayed, never queried on.
    val secondaryMuscles: String,
    val equipment: Equipment,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
    val defaultRestSec: Int?,
    val isCustom: Boolean,
    val isArchived: Boolean,
    val notes: String?,
)
