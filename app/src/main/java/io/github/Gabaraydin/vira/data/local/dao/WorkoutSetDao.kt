package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Insert
    suspend fun insert(set: WorkoutSetEntity): Long

    @Update
    suspend fun update(set: WorkoutSetEntity)

    @Delete
    suspend fun delete(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId ORDER BY position")
    fun observeForWorkout(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutSetEntity>

    @Query(
        "SELECT * FROM workout_set WHERE exerciseId = :exerciseId AND isWarmup = 0 " +
            "AND isCompleted = 1 ORDER BY completedAt",
    )
    suspend fun getCountableForExercise(exerciseId: Long): List<WorkoutSetEntity>
}
