package io.github.Gabaraydin.vira.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.Gabaraydin.vira.data.backup.BackupExporter
import io.github.Gabaraydin.vira.data.backup.BackupImportException
import io.github.Gabaraydin.vira.data.backup.BackupImporter
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class TransientState(val isBusy: Boolean = false, val outcome: BackupOutcome? = null)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val transient = MutableStateFlow(TransientState())

    val uiState: StateFlow<BackupUiState> = combine(settingsRepository.settings, transient) { settings, t ->
        BackupUiState(isBusy = t.isBusy, lastExportAt = settings.lastBackupExportAt, outcome = t.outcome)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    fun exportTo(uri: Uri) {
        transient.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val outcome = try {
                val json = exporter.export(Instant.now())
                val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream for $uri")
                stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                settingsRepository.recordBackupExport(System.currentTimeMillis())
                BackupOutcome.ExportSuccess
            } catch (e: IOException) {
                BackupOutcome.ExportFailed
            }
            transient.update { TransientState(isBusy = false, outcome = outcome) }
        }
    }

    fun importFrom(uri: Uri) {
        transient.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val outcome = try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IOException("No input stream for $uri")
                importer.import(json)
                BackupOutcome.ImportSuccess
            } catch (e: BackupImportException.InvalidFormat) {
                BackupOutcome.ImportFailedInvalidFormat
            } catch (e: BackupImportException.UnsupportedFormatVersion) {
                BackupOutcome.ImportFailedUnsupportedVersion(e.version)
            } catch (e: BackupImportException.LegacyFormatNotYetSupported) {
                BackupOutcome.ImportFailedLegacyNotSupported
            } catch (e: IOException) {
                BackupOutcome.ImportFailedGeneric
            }
            transient.update { TransientState(isBusy = false, outcome = outcome) }
        }
    }

    fun outcomeShown() {
        transient.update { it.copy(outcome = null) }
    }
}
