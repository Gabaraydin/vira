package io.github.Gabaraydin.vira.data.repository

import io.github.Gabaraydin.vira.data.local.dao.BodyMeasurementDao
import io.github.Gabaraydin.vira.data.local.entity.BodyMeasurementEntity
import io.github.Gabaraydin.vira.domain.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class BodyMeasurementRepository @Inject constructor(private val bodyMeasurementDao: BodyMeasurementDao) {

    fun observeAll(): Flow<List<BodyMeasurement>> =
        bodyMeasurementDao.observeAll().map { it.map(BodyMeasurementEntity::toDomain) }

    suspend fun getByDate(date: LocalDate): BodyMeasurement? = bodyMeasurementDao.getByDate(date)?.toDomain()

    // Upsert by design: date is UNIQUE, a second entry for the same day replaces the first.
    suspend fun upsert(measurement: BodyMeasurement): Long = bodyMeasurementDao.upsert(measurement.toEntity())

    suspend fun delete(measurement: BodyMeasurement) {
        bodyMeasurementDao.delete(measurement.toEntity())
    }
}

private fun BodyMeasurementEntity.toDomain(): BodyMeasurement = BodyMeasurement(
    id = id,
    date = date,
    weightKg = weightKg,
    heightCm = heightCm,
    waistCm = waistCm,
    neckCm = neckCm,
    hipCm = hipCm,
    bodyFatPct = bodyFatPct,
    note = note,
)

private fun BodyMeasurement.toEntity(): BodyMeasurementEntity = BodyMeasurementEntity(
    id = id,
    date = date,
    weightKg = weightKg,
    heightCm = heightCm,
    waistCm = waistCm,
    neckCm = neckCm,
    hipCm = hipCm,
    bodyFatPct = bodyFatPct,
    note = note,
)
