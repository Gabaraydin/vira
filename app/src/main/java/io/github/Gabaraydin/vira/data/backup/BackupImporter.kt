package io.github.Gabaraydin.vira.data.backup

import androidx.room.withTransaction
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.dao.BodyMeasurementDao
import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutSetDao
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class BackupImporter @Inject constructor(
    private val database: ViraDatabase,
    private val exerciseDao: ExerciseDao,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programDayExerciseDao: ProgramDayExerciseDao,
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val settingsRepository: SettingsRepository,
) {
    // Wipes every table and replaces it with the backup's contents. This is destructive by
    // design — the caller (BackupViewModel/Screen) is responsible for confirming with the
    // user before invoking it.
    suspend fun import(json: String) {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw BackupImportException.InvalidFormat(e)
        }

        // The legacy prototype's export has no formatVersion field, only a top-level
        // dayLog[] array — recognised per work/01-data-model.md.
        if (root.has("dayLog")) {
            throw BackupImportException.LegacyFormatNotYetSupported
        }

        val formatVersion = try {
            root.getInt("formatVersion")
        } catch (e: JSONException) {
            throw BackupImportException.InvalidFormat(e)
        }
        if (formatVersion > BACKUP_FORMAT_VERSION) {
            throw BackupImportException.UnsupportedFormatVersion(formatVersion)
        }

        try {
            database.withTransaction {
                database.clearAllTables()
                exerciseDao.insertAll(root.getEntityArray("exercises").map(::exerciseFromJson))
                programDao.insertAll(root.getEntityArray("programs").map(::programFromJson))
                programDayDao.insertAll(root.getEntityArray("programDays").map(::programDayFromJson))
                programDayExerciseDao.insertAll(root.getEntityArray("programDayExercises").map(::programDayExerciseFromJson))
                workoutDao.insertAll(root.getEntityArray("workouts").map(::workoutFromJson))
                workoutSetDao.insertAll(root.getEntityArray("workoutSets").map(::workoutSetFromJson))
                bodyMeasurementDao.insertAll(root.getEntityArray("bodyMeasurements").map(::bodyMeasurementFromJson))
            }
        } catch (e: JSONException) {
            throw BackupImportException.InvalidFormat(e)
        }

        val settingsJson = root.optJSONObject("settings")
        if (settingsJson != null) {
            settingsRepository.restore(appSettingsFromJson(settingsJson))
        }
    }
}
