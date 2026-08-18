package io.github.Gabaraydin.vira.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.Gabaraydin.vira.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BodyMeasurementDao {
    // date is UNIQUE; a second entry for the same day replaces the first, per spec.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(measurement: BodyMeasurementEntity): Long

    // Explicit-PK inserts (backup import) — REPLACE matches upsert's semantics, though in
    // practice import always runs against a just-cleared table so no conflict occurs.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<BodyMeasurementEntity>)

    @Delete
    suspend fun delete(measurement: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurement WHERE date = :date")
    suspend fun getByDate(date: LocalDate): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    // Full-table read for backup export.
    @Query("SELECT * FROM body_measurement ORDER BY id")
    suspend fun getAll(): List<BodyMeasurementEntity>
}
