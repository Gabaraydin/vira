package io.github.Gabaraydin.vira.data.backup

import io.github.Gabaraydin.vira.data.local.entity.BodyMeasurementEntity
import io.github.Gabaraydin.vira.data.local.entity.ExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramDayExerciseEntity
import io.github.Gabaraydin.vira.data.local.entity.ProgramEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutEntity
import io.github.Gabaraydin.vira.data.local.entity.WorkoutSetEntity
import io.github.Gabaraydin.vira.domain.model.AppLanguage
import io.github.Gabaraydin.vira.domain.model.AppSettings
import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.ThemeMode
import io.github.Gabaraydin.vira.domain.model.WeightUnit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// org.json's JSONObject.put(key, null) silently omits the key rather than storing a JSON
// null token, so "missing key" and "explicit null" are indistinguishable and both just mean
// null on read. These helpers keep every entity mapper below symmetric around that fact.
private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value != null) put(key, value)
}

private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key)) getLong(key) else null
private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key)) getInt(key) else null
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key)) getDouble(key) else null
private fun JSONObject.optStringOrNull(key: String): String? = if (has(key)) getString(key) else null

private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> = (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONArray.toObjectList(): List<JSONObject> = map { it }

fun ExerciseEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("nameEn", nameEn)
    put("nameTr", nameTr)
    put("primaryMuscle", primaryMuscle.name)
    put("secondaryMuscles", secondaryMuscles)
    put("equipment", equipment.name)
    put("isBodyweight", isBodyweight)
    put("isUnilateral", isUnilateral)
    putNullable("defaultRestSec", defaultRestSec)
    put("isCustom", isCustom)
    put("isArchived", isArchived)
    putNullable("notes", notes)
}

fun exerciseFromJson(json: JSONObject): ExerciseEntity = ExerciseEntity(
    id = json.getLong("id"),
    nameEn = json.getString("nameEn"),
    nameTr = json.getString("nameTr"),
    primaryMuscle = MuscleGroup.valueOf(json.getString("primaryMuscle")),
    secondaryMuscles = json.getString("secondaryMuscles"),
    equipment = Equipment.valueOf(json.getString("equipment")),
    isBodyweight = json.getBoolean("isBodyweight"),
    isUnilateral = json.getBoolean("isUnilateral"),
    defaultRestSec = json.optIntOrNull("defaultRestSec"),
    isCustom = json.getBoolean("isCustom"),
    isArchived = json.getBoolean("isArchived"),
    notes = json.optStringOrNull("notes"),
)

fun ProgramEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("isActive", isActive)
    put("createdAt", createdAt)
    putNullable("archivedAt", archivedAt)
}

fun programFromJson(json: JSONObject): ProgramEntity = ProgramEntity(
    id = json.getLong("id"),
    name = json.getString("name"),
    isActive = json.getBoolean("isActive"),
    createdAt = json.getLong("createdAt"),
    archivedAt = json.optLongOrNull("archivedAt"),
)

fun ProgramDayEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("programId", programId)
    put("position", position)
    put("name", name)
    put("isRest", isRest)
    putNullable("libraryCategory", libraryCategory?.name)
}

fun programDayFromJson(json: JSONObject): ProgramDayEntity = ProgramDayEntity(
    id = json.getLong("id"),
    programId = json.getLong("programId"),
    position = json.getInt("position"),
    name = json.getString("name"),
    isRest = json.getBoolean("isRest"),
    libraryCategory = json.optStringOrNull("libraryCategory")?.let { MuscleGroup.valueOf(it) },
)

fun ProgramDayExerciseEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("programDayId", programDayId)
    put("exerciseId", exerciseId)
    put("position", position)
    putNullable("supersetGroupId", supersetGroupId)
    putNullable("supersetOrder", supersetOrder)
    put("targetSets", targetSets)
    putNullable("targetRepsMin", targetRepsMin)
    putNullable("targetRepsMax", targetRepsMax)
    putNullable("targetWeightKg", targetWeightKg)
    putNullable("restSecOverride", restSecOverride)
}

fun programDayExerciseFromJson(json: JSONObject): ProgramDayExerciseEntity = ProgramDayExerciseEntity(
    id = json.getLong("id"),
    programDayId = json.getLong("programDayId"),
    exerciseId = json.getLong("exerciseId"),
    position = json.getInt("position"),
    supersetGroupId = json.optIntOrNull("supersetGroupId"),
    supersetOrder = json.optIntOrNull("supersetOrder"),
    targetSets = json.getInt("targetSets"),
    targetRepsMin = json.optIntOrNull("targetRepsMin"),
    targetRepsMax = json.optIntOrNull("targetRepsMax"),
    targetWeightKg = json.optDoubleOrNull("targetWeightKg"),
    restSecOverride = json.optIntOrNull("restSecOverride"),
)

fun WorkoutEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    putNullable("programDayId", programDayId)
    put("dayNameSnapshot", dayNameSnapshot)
    putNullable("programNameSnapshot", programNameSnapshot)
    put("date", date.toString())
    put("startedAt", startedAt)
    putNullable("finishedAt", finishedAt)
    putNullable("note", note)
}

fun workoutFromJson(json: JSONObject): WorkoutEntity = WorkoutEntity(
    id = json.getLong("id"),
    programDayId = json.optLongOrNull("programDayId"),
    dayNameSnapshot = json.getString("dayNameSnapshot"),
    programNameSnapshot = json.optStringOrNull("programNameSnapshot"),
    date = LocalDate.parse(json.getString("date")),
    startedAt = json.getLong("startedAt"),
    finishedAt = json.optLongOrNull("finishedAt"),
    note = json.optStringOrNull("note"),
)

fun WorkoutSetEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("workoutId", workoutId)
    put("exerciseId", exerciseId)
    put("position", position)
    put("setIndex", setIndex)
    put("weightKg", weightKg)
    put("reps", reps)
    putNullable("rpe", rpe)
    put("isWarmup", isWarmup)
    put("isCompleted", isCompleted)
    putNullable("completedAt", completedAt)
    putNullable("supersetGroupId", supersetGroupId)
}

fun workoutSetFromJson(json: JSONObject): WorkoutSetEntity = WorkoutSetEntity(
    id = json.getLong("id"),
    workoutId = json.getLong("workoutId"),
    exerciseId = json.getLong("exerciseId"),
    position = json.getInt("position"),
    setIndex = json.getInt("setIndex"),
    weightKg = json.getDouble("weightKg"),
    reps = json.getInt("reps"),
    rpe = json.optDoubleOrNull("rpe"),
    isWarmup = json.getBoolean("isWarmup"),
    isCompleted = json.getBoolean("isCompleted"),
    completedAt = json.optLongOrNull("completedAt"),
    supersetGroupId = json.optIntOrNull("supersetGroupId"),
)

fun BodyMeasurementEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("date", date.toString())
    put("weightKg", weightKg)
    put("heightCm", heightCm)
    putNullable("waistCm", waistCm)
    putNullable("neckCm", neckCm)
    putNullable("hipCm", hipCm)
    putNullable("bodyFatPct", bodyFatPct)
    putNullable("note", note)
}

fun bodyMeasurementFromJson(json: JSONObject): BodyMeasurementEntity = BodyMeasurementEntity(
    id = json.getLong("id"),
    date = LocalDate.parse(json.getString("date")),
    weightKg = json.getDouble("weightKg"),
    heightCm = json.getDouble("heightCm"),
    waistCm = json.optDoubleOrNull("waistCm"),
    neckCm = json.optDoubleOrNull("neckCm"),
    hipCm = json.optDoubleOrNull("hipCm"),
    bodyFatPct = json.optDoubleOrNull("bodyFatPct"),
    note = json.optStringOrNull("note"),
)

fun AppSettings.toJson(): JSONObject = JSONObject().apply {
    put("weightUnit", weightUnit.name)
    put("themeMode", themeMode.name)
    put("language", language.name)
    put("dynamicColorEnabled", dynamicColorEnabled)
    put("defaultRestSeconds", defaultRestSeconds)
    put("rpeEnabled", rpeEnabled)
    put("keepScreenOnDuringSession", keepScreenOnDuringSession)
    put("biologicalSex", biologicalSex.name)
    putNullable("lastBackupExportAt", lastBackupExportAt)
    put("hasSeenProgramSwitchExplanation", hasSeenProgramSwitchExplanation)
}

fun appSettingsFromJson(json: JSONObject): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        weightUnit = json.optStringOrNull("weightUnit")?.let { WeightUnit.valueOf(it) } ?: defaults.weightUnit,
        themeMode = json.optStringOrNull("themeMode")?.let { ThemeMode.valueOf(it) } ?: defaults.themeMode,
        language = json.optStringOrNull("language")?.let { AppLanguage.valueOf(it) } ?: defaults.language,
        dynamicColorEnabled = if (json.has("dynamicColorEnabled")) json.getBoolean("dynamicColorEnabled") else defaults.dynamicColorEnabled,
        defaultRestSeconds = json.optIntOrNull("defaultRestSeconds") ?: defaults.defaultRestSeconds,
        rpeEnabled = if (json.has("rpeEnabled")) json.getBoolean("rpeEnabled") else defaults.rpeEnabled,
        keepScreenOnDuringSession = if (json.has("keepScreenOnDuringSession")) {
            json.getBoolean("keepScreenOnDuringSession")
        } else {
            defaults.keepScreenOnDuringSession
        },
        biologicalSex = json.optStringOrNull("biologicalSex")?.let { BiologicalSex.valueOf(it) } ?: defaults.biologicalSex,
        lastBackupExportAt = json.optLongOrNull("lastBackupExportAt"),
        hasSeenProgramSwitchExplanation = if (json.has("hasSeenProgramSwitchExplanation")) {
            json.getBoolean("hasSeenProgramSwitchExplanation")
        } else {
            defaults.hasSeenProgramSwitchExplanation
        },
    )
}

fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
    val array = JSONArray()
    forEach { array.put(transform(it)) }
    return array
}

fun JSONObject.getEntityArray(key: String): List<JSONObject> = getJSONArray(key).toObjectList()
