package io.github.Gabaraydin.vira.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import io.github.Gabaraydin.vira.data.local.entity.BodyMeasurementEntity
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.ThemeMode
import io.github.Gabaraydin.vira.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: ViraDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var exporter: BackupExporter
    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ViraDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val folder = tempFolder.newFolder()
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(folder, "settings.preferences_pb") },
        )
        settingsRepository = SettingsRepository(store)

        exporter = BackupExporter(
            context, database.exerciseDao(), database.programDao(), database.programDayDao(),
            database.programDayExerciseDao(), database.workoutDao(), database.workoutSetDao(),
            database.bodyMeasurementDao(), settingsRepository,
        )
        importer = BackupImporter(
            database, database.exerciseDao(), database.programDao(), database.programDayDao(),
            database.programDayExerciseDao(), database.workoutDao(), database.workoutSetDao(),
            database.bodyMeasurementDao(), settingsRepository,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedFullDataset() {
        val exerciseId = database.exerciseDao().insert(
            ExerciseEntity(
                nameEn = "Bench Press", nameTr = "Bench Press", primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = "TRICEPS", equipment = Equipment.BARBELL, isBodyweight = false,
                isUnilateral = false, defaultRestSec = 120, isCustom = false, isArchived = false, notes = null,
            ),
        )
        val programId = database.programDao().insert(
            ProgramEntity(name = "Push Pull Legs", isActive = true, createdAt = 1_000L, archivedAt = null),
        )
        val dayId = database.programDayDao().insert(
            ProgramDayEntity(programId = programId, position = 0, name = "Push", isRest = false, libraryCategory = MuscleGroup.CHEST),
        )
        database.programDayExerciseDao().insert(
            ProgramDayExerciseEntity(
                programDayId = dayId, exerciseId = exerciseId, position = 0, supersetGroupId = null,
                supersetOrder = null, targetSets = 3, targetRepsMin = 8, targetRepsMax = 12,
                targetWeightKg = 60.0, restSecOverride = null,
            ),
        )
        val workoutId = database.workoutDao().insert(
            WorkoutEntity(
                programDayId = dayId, dayNameSnapshot = "Push", programNameSnapshot = "Push Pull Legs",
                date = LocalDate.of(2026, 8, 1), startedAt = 2_000L, finishedAt = 3_000L, note = "felt strong",
            ),
        )
        database.workoutSetDao().insert(
            WorkoutSetEntity(
                workoutId = workoutId, exerciseId = exerciseId, position = 0, setIndex = 1, weightKg = 60.0,
                reps = 10, rpe = 8.0, isWarmup = false, isCompleted = true, completedAt = 2_500L, supersetGroupId = null,
            ),
        )
        database.bodyMeasurementDao().upsert(
            BodyMeasurementEntity(
                date = LocalDate.of(2026, 8, 1), weightKg = 80.0, heightCm = 180.0, waistCm = 85.0,
                neckCm = 40.0, hipCm = null, bodyFatPct = 18.5, note = null,
            ),
        )
        settingsRepository.setWeightUnit(WeightUnit.LB)
        settingsRepository.setThemeMode(ThemeMode.LIGHT)
        settingsRepository.setBiologicalSex(BiologicalSex.FEMALE)
        settingsRepository.setDefaultRestSeconds(150)
    }

    @Test
    fun exportThenImportPreservesEveryTableAndExplicitIds() = runTest {
        seedFullDataset()

        val beforeExercises = database.exerciseDao().getAll()
        val beforePrograms = database.programDao().getAll()
        val beforeDays = database.programDayDao().getAll()
        val beforeEntries = database.programDayExerciseDao().getAll()
        val beforeWorkouts = database.workoutDao().getAll()
        val beforeSets = database.workoutSetDao().getAll()
        val beforeMeasurements = database.bodyMeasurementDao().getAll()
        val beforeSettings = settingsRepository.settings.first()

        val json = exporter.export(Instant.parse("2026-08-17T10:00:00Z"))

        // Wipes and reloads from the JSON we just produced — round-trips through the exact
        // same code path a real device-to-device restore would use.
        importer.import(json)

        assertEquals(beforeExercises, database.exerciseDao().getAll())
        assertEquals(beforePrograms, database.programDao().getAll())
        assertEquals(beforeDays, database.programDayDao().getAll())
        assertEquals(beforeEntries, database.programDayExerciseDao().getAll())
        assertEquals(beforeWorkouts, database.workoutDao().getAll())
        assertEquals(beforeSets, database.workoutSetDao().getAll())
        assertEquals(beforeMeasurements, database.bodyMeasurementDao().getAll())
        assertEquals(beforeSettings, settingsRepository.settings.first())
    }

    @Test
    fun importRejectsGarbageJson() = runTest {
        try {
            importer.import("not json at all")
            fail("expected InvalidFormat")
        } catch (expected: BackupImportException.InvalidFormat) {
            // expected
        }
    }

    @Test
    fun importRejectsMissingFormatVersion() = runTest {
        try {
            importer.import("""{"exercises":[]}""")
            fail("expected InvalidFormat")
        } catch (expected: BackupImportException.InvalidFormat) {
            // expected
        }
    }

    @Test
    fun importRejectsAFormatVersionNewerThanSupported() = runTest {
        val json = exporter.export(Instant.now())
        val bumped = org.json.JSONObject(json).put("formatVersion", BACKUP_FORMAT_VERSION + 1).toString()

        try {
            importer.import(bumped)
            fail("expected UnsupportedFormatVersion")
        } catch (expected: BackupImportException.UnsupportedFormatVersion) {
            assertEquals(BACKUP_FORMAT_VERSION + 1, expected.version)
        }
    }

    @Test
    fun importDetectsTheLegacyPrototypeFormatAndRefusesIt() = runTest {
        try {
            importer.import("""{"dayLog":[],"cycle":[]}""")
            fail("expected LegacyFormatNotYetSupported")
        } catch (expected: BackupImportException.LegacyFormatNotYetSupported) {
            // expected — no sample of the legacy format exists yet to implement conversion against
        }
    }

    @Test
    fun aFailedImportLeavesExistingDataIntact() = runTest {
        seedFullDataset()
        val before = database.exerciseDao().getAll()

        try {
            // formatVersion is fine but the exercises array is malformed — should roll back
            // the whole transaction, not leave the database half-cleared.
            importer.import("""{"formatVersion":1,"exercises":[{"id":"not-a-number"}]}""")
            fail("expected InvalidFormat")
        } catch (expected: BackupImportException.InvalidFormat) {
            // expected
        }

        assertEquals(before, database.exerciseDao().getAll())
        assertTrue(before.isNotEmpty())
    }
}
