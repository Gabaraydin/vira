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

    // For the active-workout screen's "previous session" reference row: every set this
    // exercise had in the most recent *other* finished workout that logged it at all.
    @Query(
        """
        SELECT ws.* FROM workout_set ws
        INNER JOIN workout w ON w.id = ws.workoutId
        WHERE ws.exerciseId = :exerciseId AND ws.workoutId != :excludeWorkoutId AND w.finishedAt IS NOT NULL
        AND ws.workoutId = (
            SELECT ws2.workoutId FROM workout_set ws2
            INNER JOIN workout w2 ON w2.id = ws2.workoutId
            WHERE ws2.exerciseId = :exerciseId AND ws2.workoutId != :excludeWorkoutId AND w2.finishedAt IS NOT NULL
            ORDER BY w2.date DESC, w2.startedAt DESC
            LIMIT 1
        )
        ORDER BY ws.position
        """,
    )
    suspend fun getPreviousSessionSets(exerciseId: Long, excludeWorkoutId: Long): List<WorkoutSetEntity>

    @Query("SELECT MAX(position) FROM workout_set WHERE workoutId = :workoutId")
    suspend fun getMaxPosition(workoutId: Long): Int?
}
