package io.github.Gabaraydin.vira.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.Gabaraydin.vira.data.local.ViraDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {

    private lateinit var database: ViraDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ViraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database, database.workoutDao(), database.workoutSetDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun startingASessionWithNoneInProgressSucceeds() = runTest {
        val id = repository.startSession(
            programDayId = null,
            dayNameSnapshot = "Ad-hoc",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 1),
            startedAt = 1000,
        )

        assertNotNull(database.workoutDao().getById(id))
        assertNull(requireNotNull(database.workoutDao().getById(id)).finishedAt)
    }

    @Test
    fun startingASecondSessionWhileOneIsUnfinishedIsRejected() = runTest {
        repository.startSession(
            programDayId = null,
            dayNameSnapshot = "First",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 1),
            startedAt = 1000,
        )

        try {
            repository.startSession(
                programDayId = null,
                dayNameSnapshot = "Second",
                programNameSnapshot = null,
                date = LocalDate.of(2026, 1, 1),
                startedAt = 2000,
            )
            fail("expected an IllegalStateException")
        } catch (expected: IllegalStateException) {
            // expected
        }

        assertEquals(1, database.workoutDao().observeAll().first().size)
    }

    @Test
    fun startingANewSessionAfterFinishingTheOldOneSucceeds() = runTest {
        val firstId = repository.startSession(
            programDayId = null,
            dayNameSnapshot = "First",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 1),
            startedAt = 1000,
        )
        repository.finishSession(firstId, finishedAt = 1500)

        val secondId = repository.startSession(
            programDayId = null,
            dayNameSnapshot = "Second",
            programNameSnapshot = null,
            date = LocalDate.of(2026, 1, 2),
            startedAt = 2000,
        )

        assertNull(requireNotNull(database.workoutDao().getById(secondId)).finishedAt)
        assertNotNull(requireNotNull(database.workoutDao().getById(firstId)).finishedAt)
    }
}
