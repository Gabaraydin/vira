package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDayDao {
    @Insert
    suspend fun insert(day: ProgramDayEntity): Long

    // Explicit-PK inserts (backup import) — preserves ids so program_day_exercise/workout
    // FKs still resolve after a round-trip.
    @Insert
    suspend fun insertAll(days: List<ProgramDayEntity>)

    @Update
    suspend fun update(day: ProgramDayEntity)

    @Delete
    suspend fun delete(day: ProgramDayEntity)

    @Query("SELECT * FROM program_day WHERE id = :id")
    suspend fun getById(id: Long): ProgramDayEntity?

    @Query("SELECT * FROM program_day WHERE programId = :programId ORDER BY position")
    fun observeForProgram(programId: Long): Flow<List<ProgramDayEntity>>

    @Query("SELECT * FROM program_day WHERE programId = :programId ORDER BY position")
    suspend fun getForProgram(programId: Long): List<ProgramDayEntity>

    // Full-table read for backup export — across every program, not just the active one.
    @Query("SELECT * FROM program_day ORDER BY id")
    suspend fun getAll(): List<ProgramDayEntity>
}
