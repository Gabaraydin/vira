package io.github.Gabaraydin.vira.domain.model

data class AppSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val defaultRestSeconds: Int = 90,
    val rpeEnabled: Boolean = false,
    val keepScreenOnDuringSession: Boolean = true,
    // null means no export has ever been made; the 30-day reminder is computed from this.
    val lastBackupExportAt: Long? = null,
)
