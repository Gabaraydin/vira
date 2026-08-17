package io.github.Gabaraydin.vira.domain.seed

import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCsvParserTest {

    private val header = "name_en,name_tr,primary_muscle,secondary_muscles,equipment,is_bodyweight,is_unilateral"

    @Test
    fun `header-only csv produces no rows`() {
        assertTrue(parseExerciseCsv(header).isEmpty())
    }

    @Test
    fun `parses a plain row with no secondary muscles`() {
        val csv = "$header\nOverhead Press,Askeri Pres,SHOULDERS,,BARBELL,0,0"
        val rows = parseExerciseCsv(csv)

        assertEquals(1, rows.size)
        val row = rows[0]
        assertEquals("Overhead Press", row.nameEn)
        assertEquals("Askeri Pres", row.nameTr)
        assertEquals(MuscleGroup.SHOULDERS, row.primaryMuscle)
        assertTrue(row.secondaryMuscles.isEmpty())
        assertEquals(Equipment.BARBELL, row.equipment)
        assertEquals(false, row.isBodyweight)
        assertEquals(false, row.isUnilateral)
    }

    @Test
    fun `parses a quoted two-item secondary muscle list`() {
        val csv = "$header\nBarbell Bench Press,Bench Press,CHEST,\"TRICEPS,SHOULDERS\",BARBELL,0,0"
        val row = parseExerciseCsv(csv).single()

        assertEquals(listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), row.secondaryMuscles)
    }

    @Test
    fun `parses a quoted three-item secondary muscle list`() {
        val csv = "$header\nDeadlift,Deadlift,BACK,\"HAMSTRINGS,GLUTES,FOREARMS\",BARBELL,0,0"
        val row = parseExerciseCsv(csv).single()

        assertEquals(
            listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.FOREARMS),
            row.secondaryMuscles,
        )
    }

    @Test
    fun `bodyweight and unilateral flags parse from 1`() {
        val csv = "$header\nPistol Squat,Pistol Squat,QUADS,\"GLUTES,CORE\",BODYWEIGHT,1,1"
        val row = parseExerciseCsv(csv).single()

        assertEquals(true, row.isBodyweight)
        assertEquals(true, row.isUnilateral)
    }

    @Test
    fun `parses multiple rows in order`() {
        val csv = "$header\n" +
            "Overhead Press,Askeri Pres,SHOULDERS,,BARBELL,0,0\n" +
            "Push-up,Şınav,CHEST,\"TRICEPS,CORE\",BODYWEIGHT,1,0"
        val rows = parseExerciseCsv(csv)

        assertEquals(2, rows.size)
        assertEquals("Overhead Press", rows[0].nameEn)
        assertEquals("Push-up", rows[1].nameEn)
    }

    @Test
    fun `blank lines are ignored`() {
        val csv = "$header\n\nOverhead Press,Askeri Pres,SHOULDERS,,BARBELL,0,0\n\n"
        assertEquals(1, parseExerciseCsv(csv).size)
    }

    @Test
    fun `an unknown muscle group is rejected`() {
        val csv = "$header\nMystery Move,Mystery Move,NOT_A_MUSCLE,,BARBELL,0,0"
        assertThrows(IllegalArgumentException::class.java) { parseExerciseCsv(csv) }
    }

    @Test
    fun `a row with the wrong number of columns is rejected`() {
        val csv = "$header\nOverhead Press,Askeri Pres,SHOULDERS,BARBELL,0,0"
        assertThrows(IllegalArgumentException::class.java) { parseExerciseCsv(csv) }
    }
}
