package io.github.Gabaraydin.vira.data.backup

sealed class BackupImportException(message: String) : Exception(message) {
    class InvalidFormat(cause: Throwable) :
        BackupImportException("The selected file is not a valid Vira backup: ${cause.message}")

    class UnsupportedFormatVersion(val version: Int) :
        BackupImportException("Backup format version $version is newer than this app supports")

    // The legacy HTML prototype's dayLog[]/cycle[]/exercises[]/sessions[]/measures[] format
    // is detected but not yet converted — see work/01-data-model.md and job-context.md §5:
    // no sample export of that format exists to verify field names against.
    object LegacyFormatNotYetSupported :
        BackupImportException("Importing the old prototype's export format isn't supported yet")
}
