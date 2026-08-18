package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class WorkoutSetWithDate(@Embedded val set: WorkoutSetEntity, val date: LocalDate)

data class ExerciseLastPerformed(val exerciseId: Long, val lastDate: LocalDate)

@Dao
interface WorkoutSetDao {
    @Insert
    suspend fun insert(set: WorkoutSetEntity): Long

    // Explicit-PK inserts (backup import).
    @Insert
    suspend fun insertAll(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun update(set: WorkoutSetEntity)

    @Delete
    suspend fun delete(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId ORDER BY position")
    fun observeForWorkout(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutSetEntity>

    // Full-table read for backup export.
    @Query("SELECT * FROM workout_set ORDER BY id")
    suspend fun getAll(): List<WorkoutSetEntity>

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

    // Every completed, non-warm-up set this exercise has in a *different*, already
    // finished workout — the history a just-finished session's sets get checked against
    // to flag a new PR on the Workout Summary screen.
    @Query(
        """
        SELECT ws.* FROM workout_set ws
        INNER JOIN workout w ON w.id = ws.workoutId
        WHERE ws.exerciseId = :exerciseId AND ws.workoutId != :excludeWorkoutId
        AND w.finishedAt IS NOT NULL AND ws.isCompleted = 1 AND ws.isWarmup = 0
        """,
    )
    suspend fun getPriorCompletedSets(exerciseId: Long, excludeWorkoutId: Long): List<WorkoutSetEntity>

    // Exercise Detail's e1RM chart, PR table, and session history all come from this one
    // query: every set logged for the exercise in a finished workout, oldest first, each
    // tagged with its workout's date so the caller can group by session.
    @Query(
        """
        SELECT ws.*, w.date AS date FROM workout_set ws
        INNER JOIN workout w ON w.id = ws.workoutId
        WHERE ws.exerciseId = :exerciseId AND w.finishedAt IS NOT NULL
        ORDER BY w.date, w.startedAt
        """,
    )
    fun observeAllSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetWithDate>>

    // The Exercise Library list's "last performed" column, for every exercise at once —
    // avoids one query per row for what could be ~120 rows.
    @Query(
        """
        SELECT ws.exerciseId AS exerciseId, MAX(w.date) AS lastDate
        FROM workout_set ws
        INNER JOIN workout w ON w.id = ws.workoutId
        WHERE w.finishedAt IS NOT NULL AND ws.isCompleted = 1
        GROUP BY ws.exerciseId
        """,
    )
    fun observeLastPerformedDates(): Flow<List<ExerciseLastPerformed>>
}
