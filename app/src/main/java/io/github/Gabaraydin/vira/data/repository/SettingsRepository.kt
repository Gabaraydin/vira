package io.github.Gabaraydin.vira.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.Gabaraydin.vira.domain.model.AppLanguage
import io.github.Gabaraydin.vira.domain.model.AppSettings
import io.github.Gabaraydin.vira.domain.model.ThemeMode
import io.github.Gabaraydin.vira.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private object Keys {
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
    val RPE_ENABLED = booleanPreferencesKey("rpe_enabled")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_during_session")
    val LAST_BACKUP_EXPORT_AT = longPreferencesKey("last_backup_export_at")
}

class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { WeightUnit.valueOf(it) } ?: defaults.weightUnit,
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: defaults.themeMode,
            language = prefs[Keys.LANGUAGE]?.let { AppLanguage.valueOf(it) } ?: defaults.language,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: defaults.dynamicColorEnabled,
            defaultRestSeconds = prefs[Keys.DEFAULT_REST_SECONDS] ?: defaults.defaultRestSeconds,
            rpeEnabled = prefs[Keys.RPE_ENABLED] ?: defaults.rpeEnabled,
            keepScreenOnDuringSession = prefs[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOnDuringSession,
            lastBackupExportAt = prefs[Keys.LAST_BACKUP_EXPORT_AT],
        )
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        require(seconds > 0) { "defaultRestSeconds must be positive, was $seconds" }
        dataStore.edit { it[Keys.DEFAULT_REST_SECONDS] = seconds }
    }

    suspend fun setRpeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.RPE_ENABLED] = enabled }
    }

    suspend fun setKeepScreenOnDuringSession(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun recordBackupExport(at: Long) {
        dataStore.edit { it[Keys.LAST_BACKUP_EXPORT_AT] = at }
    }
}
