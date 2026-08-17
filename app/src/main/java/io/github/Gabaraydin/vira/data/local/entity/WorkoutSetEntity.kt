package io.github.Gabaraydin.vira.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseId", "isWarmup", "isCompleted"]),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val setIndex: Int,
    // Added weight only for bodyweight exercises; never includes bodyweight itself.
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val completedAt: Long?,
    // Copied from the plan for display; not a foreign key, just a superset label hint.
    val supersetGroupId: Int?,
)
