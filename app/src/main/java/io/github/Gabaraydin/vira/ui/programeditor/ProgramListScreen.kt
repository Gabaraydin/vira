package io.github.Gabaraydin.vira.ui.programeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.model.Program
import kotlinx.coroutines.launch

@Composable
fun ProgramListRoute(
    onOpenDayEditor: (Long) -> Unit,
    viewModel: ProgramListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val duplicateFormat = stringResource(R.string.program_duplicate_name_format)

    var showCreateDialog by remember { mutableStateOf(false) }
    var switchExplanationTarget by remember { mutableStateOf<Program?>(null) }

    ProgramListScreen(
        uiState = uiState,
        onCreateClick = { showCreateDialog = true },
        onSetActive = { program ->
            scope.launch {
                if (viewModel.shouldShowSwitchExplanation()) {
                    switchExplanationTarget = program
                } else {
                    viewModel.confirmSetActiveProgram(program)
                }
            }
        },
        onDuplicate = { program -> viewModel.duplicateProgram(program, duplicateFormat.format(program.name)) },
        onArchive = viewModel::archiveProgram,
        onOpenDayEditor = onOpenDayEditor,
    )

    if (showCreateDialog) {
        CreateProgramDialog(
            onConfirm = { name -> viewModel.createProgram(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false },
        )
    }

    switchExplanationTarget?.let { program ->
        ProgramSwitchExplanationDialog(
            onConfirm = { viewModel.confirmSetActiveProgram(program); switchExplanationTarget = null },
            onDismiss = { switchExplanationTarget = null },
        )
    }
}

@Composable
private fun ProgramListScreen(
    uiState: ProgramListUiState,
    onCreateClick: () -> Unit,
    onSetActive: (Program) -> Unit,
    onDuplicate: (Program) -> Unit,
    onArchive: (Program) -> Unit,
    onOpenDayEditor: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.program_list_create))
        }
        Spacer(Modifier.height(16.dp))

        if (uiState.programs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.program_list_empty))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.programs, key = { it.id }) { program ->
                    ProgramRow(program, onSetActive, onDuplicate, onArchive, onOpenDayEditor)
                }
            }
        }
    }
}

@Composable
private fun ProgramRow(
    program: Program,
    onSetActive: (Program) -> Unit,
    onDuplicate: (Program) -> Unit,
    onArchive: (Program) -> Unit,
    onOpenDayEditor: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(program.name, style = MaterialTheme.typography.headlineMedium)
                if (program.isActive) {
                    Text(
                        stringResource(R.string.program_list_active_badge),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onOpenDayEditor(program.id) }) {
                    Text(stringResource(R.string.program_list_action_edit_days))
                }
                if (!program.isActive) {
                    TextButton(onClick = { onSetActive(program) }) {
                        Text(stringResource(R.string.program_list_action_set_active))
                    }
                }
                TextButton(onClick = { onDuplicate(program) }) {
                    Text(stringResource(R.string.program_list_action_duplicate))
                }
                TextButton(onClick = { onArchive(program) }) {
                    Text(stringResource(R.string.program_list_action_archive))
                }
            }
        }
    }
}

@Composable
private fun CreateProgramDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_list_create_dialog_title)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun ProgramSwitchExplanationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_switch_explanation_title)) },
        text = { Text(stringResource(R.string.program_switch_explanation_body)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
