package io.github.Gabaraydin.vira.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
private val DISPLAY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")

@Composable
fun BackupRoute(viewModel: BackupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackupScreen(
        uiState = uiState,
        onExport = viewModel::exportTo,
        onImport = viewModel::importFrom,
        onOutcomeShown = viewModel::outcomeShown,
    )
}

@Composable
private fun BackupScreen(
    uiState: BackupUiState,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onOutcomeShown: () -> Unit,
) {
    var showImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImport)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.headlineMedium)

        Text(
            uiState.lastExportAt?.let { stringResource(R.string.backup_last_export_format, formatTimestamp(it)) }
                ?: stringResource(R.string.backup_last_export_never),
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = { exportLauncher.launch(suggestedFileName()) },
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.backup_export_button)) }

        OutlinedButton(
            onClick = { showImportConfirm = true },
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.backup_import_button)) }

        if (uiState.isBusy) {
            CircularProgressIndicator()
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                ) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }

    uiState.outcome?.let { outcome ->
        AlertDialog(
            onDismissRequest = onOutcomeShown,
            title = { Text(outcomeTitle(outcome)) },
            text = { Text(outcomeBody(outcome)) },
            confirmButton = {
                TextButton(onClick = onOutcomeShown) { Text(stringResource(R.string.dialog_confirm)) }
            },
        )
    }
}

@Composable
private fun outcomeTitle(outcome: BackupOutcome): String = stringResource(
    when (outcome) {
        BackupOutcome.ExportSuccess -> R.string.backup_export_success_title
        BackupOutcome.ExportFailed -> R.string.backup_export_failed_title
        BackupOutcome.ImportSuccess -> R.string.backup_import_success_title
        else -> R.string.backup_import_failed_title
    },
)

@Composable
private fun outcomeBody(outcome: BackupOutcome): String = when (outcome) {
    BackupOutcome.ExportSuccess -> stringResource(R.string.backup_export_success_body)
    BackupOutcome.ExportFailed -> stringResource(R.string.backup_export_failed_body)
    BackupOutcome.ImportSuccess -> stringResource(R.string.backup_import_success_body)
    BackupOutcome.ImportFailedInvalidFormat -> stringResource(R.string.backup_import_failed_invalid_format)
    is BackupOutcome.ImportFailedUnsupportedVersion ->
        stringResource(R.string.backup_import_failed_unsupported_version, outcome.version)
    BackupOutcome.ImportFailedLegacyNotSupported -> stringResource(R.string.backup_import_failed_legacy_not_supported)
    BackupOutcome.ImportFailedGeneric -> stringResource(R.string.backup_import_failed_generic)
}

private fun suggestedFileName(): String =
    "vira-backup-${EXPORT_TIMESTAMP_FORMAT.format(Instant.now().atZone(ZoneId.systemDefault()))}.json"

private fun formatTimestamp(epochMillis: Long): String =
    DISPLAY_TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
