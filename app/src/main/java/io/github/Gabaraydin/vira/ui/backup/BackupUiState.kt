package io.github.Gabaraydin.vira.ui.backup

data class BackupUiState(
    val isBusy: Boolean = false,
    val lastExportAt: Long? = null,
    val outcome: BackupOutcome? = null,
)

// Never carries raw exception text — every variant maps to a strings.xml resource in the
// screen, per the project's "no user-facing string outside strings.xml" rule.
sealed class BackupOutcome {
    data object ExportSuccess : BackupOutcome()
    data object ExportFailed : BackupOutcome()
    data object ImportSuccess : BackupOutcome()
    data object ImportFailedInvalidFormat : BackupOutcome()
    data class ImportFailedUnsupportedVersion(val version: Int) : BackupOutcome()
    data object ImportFailedLegacyNotSupported : BackupOutcome()
    data object ImportFailedGeneric : BackupOutcome()
}
