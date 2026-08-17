package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDayExerciseDao {
    @Insert
    suspend fun insert(entry: ProgramDayExerciseEntity): Long

    @Update
    suspend fun update(entry: ProgramDayExerciseEntity)

    @Delete
    suspend fun delete(entry: ProgramDayExerciseEntity)

    @Query("SELECT * FROM program_day_exercise WHERE programDayId = :programDayId ORDER BY position")
    fun observeForDay(programDayId: Long): Flow<List<ProgramDayExerciseEntity>>

    @Query("SELECT * FROM program_day_exercise WHERE programDayId = :programDayId ORDER BY position")
    suspend fun getForDay(programDayId: Long): List<ProgramDayExerciseEntity>

    @Query("SELECT * FROM program_day_exercise WHERE id = :id")
    suspend fun getById(id: Long): ProgramDayExerciseEntity?
}
