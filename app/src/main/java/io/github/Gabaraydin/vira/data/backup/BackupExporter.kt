package io.github.Gabaraydin.vira.data.backup

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.Gabaraydin.vira.data.local.dao.BodyMeasurementDao
import io.github.Gabaraydin.vira.data.local.dao.ExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayDao
import io.github.Gabaraydin.vira.data.local.dao.ProgramDayExerciseDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutDao
import io.github.Gabaraydin.vira.data.local.dao.WorkoutSetDao
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

const val BACKUP_FORMAT_VERSION = 1

class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programDayExerciseDao: ProgramDayExerciseDao,
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun export(exportedAt: Instant): String {
        val payload = JSONObject().apply {
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAt", exportedAt.toString())
            put("appVersion", appVersionName())
            put("exercises", exerciseDao.getAll().toJsonArray { it.toJson() })
            put("programs", programDao.getAll().toJsonArray { it.toJson() })
            put("programDays", programDayDao.getAll().toJsonArray { it.toJson() })
            put("programDayExercises", programDayExerciseDao.getAll().toJsonArray { it.toJson() })
            put("workouts", workoutDao.getAll().toJsonArray { it.toJson() })
            put("workoutSets", workoutSetDao.getAll().toJsonArray { it.toJson() })
            put("bodyMeasurements", bodyMeasurementDao.getAll().toJsonArray { it.toJson() })
            put("settings", settingsRepository.settings.first().toJson())
        }
        return payload.toString(2)
    }

    private fun appVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}
