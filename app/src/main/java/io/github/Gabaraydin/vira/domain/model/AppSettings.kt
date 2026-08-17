package io.github.Gabaraydin.vira.domain.model

data class AppSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val defaultRestSeconds: Int = 90,
    val rpeEnabled: Boolean = false,
    val keepScreenOnDuringSession: Boolean = true,
    // Picks the US Navy body-fat formula variant on the Body screen. No dedicated
    // Settings screen exists yet (#22), so this is set from the Body screen itself.
    val biologicalSex: BiologicalSex = BiologicalSex.MALE,
    // null means no export has ever been made; the 30-day reminder is computed from this.
    val lastBackupExportAt: Long? = null,
    // Gates the one-time "history is preserved" explanation shown on the first program switch.
    val hasSeenProgramSwitchExplanation: Boolean = false,
)
