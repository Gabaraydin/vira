package io.github.Gabaraydin.vira.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.Gabaraydin.vira.domain.model.AppLanguage
import io.github.Gabaraydin.vira.domain.model.AppSettings
import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import io.github.Gabaraydin.vira.domain.model.ThemeMode
import io.github.Gabaraydin.vira.domain.model.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

// Runs on-device (real Linux filesystem) rather than as a JVM unit test: DataStore's
// write-then-rename on Windows NTFS is flaky under rapid sequential edits in a JVM test,
// same reason the Room repository tests in this package are instrumented too.
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newRepository(): SettingsRepository {
        // newFolder() must be called once, outside the lambda: produceFile() can be
        // invoked more than once, and a fresh folder on each call makes DataStore resolve
        // a different path each time, which breaks its internal write-then-rename.
        val folder = tempFolder.newFolder()
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(folder, "settings.preferences_pb") },
        )
        return SettingsRepository(store)
    }

    @Test
    fun defaultsMatchTheProductDecisionsBeforeAnythingIsSet() = runBlocking {
        val settings = newRepository().settings.first()

        assertEquals(AppSettings(), settings)
        assertEquals(WeightUnit.KG, settings.weightUnit)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(AppLanguage.SYSTEM, settings.language)
        assertEquals(false, settings.dynamicColorEnabled)
        assertEquals(90, settings.defaultRestSeconds)
        assertEquals(false, settings.rpeEnabled)
        assertEquals(true, settings.keepScreenOnDuringSession)
        assertNull(settings.lastBackupExportAt)
        assertEquals(false, settings.hasSeenProgramSwitchExplanation)
        assertEquals(BiologicalSex.MALE, settings.biologicalSex)
    }

    @Test
    fun settingBiologicalSexPersists() = runBlocking {
        val repository = newRepository()

        repository.setBiologicalSex(BiologicalSex.FEMALE)

        assertEquals(BiologicalSex.FEMALE, repository.settings.first().biologicalSex)
    }

    @Test
    fun markingTheProgramSwitchExplanationSeenPersists() = runBlocking {
        val repository = newRepository()

        repository.markProgramSwitchExplanationSeen()

        assertEquals(true, repository.settings.first().hasSeenProgramSwitchExplanation)
    }

    @Test
    fun settingAValuePersistsAndIsReflectedInTheNextRead() = runBlocking {
        val repository = newRepository()

        repository.setWeightUnit(WeightUnit.LB)
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setLanguage(AppLanguage.TR)
        repository.setDynamicColorEnabled(true)
        repository.setDefaultRestSeconds(120)
        repository.setRpeEnabled(true)
        repository.setKeepScreenOnDuringSession(false)
        repository.recordBackupExport(1_700_000_000_000)

        val settings = repository.settings.first()
        assertEquals(WeightUnit.LB, settings.weightUnit)
        assertEquals(ThemeMode.LIGHT, settings.themeMode)
        assertEquals(AppLanguage.TR, settings.language)
        assertEquals(true, settings.dynamicColorEnabled)
        assertEquals(120, settings.defaultRestSeconds)
        assertEquals(true, settings.rpeEnabled)
        assertEquals(false, settings.keepScreenOnDuringSession)
        assertEquals(1_700_000_000_000L, settings.lastBackupExportAt)
    }

    @Test
    fun zeroDefaultRestSecondsIsRejected() = runBlocking {
        val repository = newRepository()
        try {
            repository.setDefaultRestSeconds(0)
            fail("expected an IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun negativeDefaultRestSecondsIsRejected() = runBlocking {
        val repository = newRepository()
        try {
            repository.setDefaultRestSeconds(-30)
            fail("expected an IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
