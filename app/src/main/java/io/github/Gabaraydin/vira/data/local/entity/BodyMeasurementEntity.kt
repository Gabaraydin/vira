package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "body_measurement", indices = [Index(value = ["date"], unique = true)])
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // One measurement per day; a second entry for the same date overwrites the first.
    val date: LocalDate,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double?,
    val neckCm: Double?,
    // Required for the female body-fat formula.
    val hipCm: Double?,
    val bodyFatPct: Double?,
    val note: String?,
)
