package io.github.Gabaraydin.vira.domain.seed

import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup

// name_en,name_tr,primary_muscle,secondary_muscles,equipment,is_bodyweight,is_unilateral
// secondary_muscles is the only field ever quoted, since it's the only one that can
// contain a comma (e.g. "TRICEPS,SHOULDERS"). No escaped quotes appear in the seed data.
fun parseExerciseCsv(csvText: String): List<SeedExercise> {
    val lines = csvText.lines().filter { it.isNotBlank() }
    require(lines.isNotEmpty()) { "CSV text must contain at least a header row" }

    return lines.drop(1).map { line -> parseExerciseCsvLine(line) }
}

private fun parseExerciseCsvLine(line: String): SeedExercise {
    val fields = splitCsvLine(line)
    require(fields.size == 7) { "expected 7 columns, got ${fields.size} in line: $line" }

    return SeedExercise(
        nameEn = fields[0].trim(),
        nameTr = fields[1].trim(),
        primaryMuscle = MuscleGroup.valueOf(fields[2].trim()),
        secondaryMuscles = parseMuscleList(fields[3]),
        equipment = Equipment.valueOf(fields[4].trim()),
        isBodyweight = fields[5].trim() == "1",
        isUnilateral = fields[6].trim() == "1",
    )
}

private fun parseMuscleList(field: String): List<MuscleGroup> {
    val trimmed = field.trim()
    if (trimmed.isEmpty()) return emptyList()
    return trimmed.split(",").map { MuscleGroup.valueOf(it.trim()) }
}

private fun splitCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false

    for (c in line) {
        when {
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
    }
    fields.add(current.toString())
    return fields
}
