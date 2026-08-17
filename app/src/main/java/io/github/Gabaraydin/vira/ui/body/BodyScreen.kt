package io.github.Gabaraydin.vira.ui.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.calculations.BodyFatCategory
import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import io.github.Gabaraydin.vira.ui.charts.LineChart

@Composable
fun BodyRoute(viewModel: BodyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BodyScreen(
        uiState = uiState,
        onSexChange = viewModel::setSex,
        onSaveEntry = viewModel::saveEntry,
        onDeleteEntry = viewModel::deleteEntry,
    )
}

@Composable
private fun BodyScreen(
    uiState: BodyUiState,
    onSexChange: (BiologicalSex) -> Unit,
    onSaveEntry: (Double, Double, Double, Double, Double?) -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    val weightYFormat = stringResource(R.string.body_weight_chart_y_format)
    val bodyFatYFormat = stringResource(R.string.body_fat_chart_y_format)

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stringResource(R.string.body_title), style = MaterialTheme.typography.headlineMedium) }

        item {
            EntryForm(sex = uiState.sex, onSexChange = onSexChange, onSaveEntry = onSaveEntry)
        }

        uiState.latestResult?.let { result ->
            item { ResultCard(result) }
        }

        item {
            Column {
                Text(stringResource(R.string.body_weight_chart_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LineChart(
                    points = uiState.weightChartPoints,
                    emptyStateText = stringResource(R.string.body_weight_chart_empty),
                    yAxisFormatter = { String.format(weightYFormat, formatNumber(it)) },
                )
            }
        }

        item {
            Column {
                Text(stringResource(R.string.body_fat_chart_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LineChart(
                    points = uiState.bodyFatChartPoints,
                    emptyStateText = stringResource(R.string.body_fat_chart_empty),
                    yAxisFormatter = { String.format(bodyFatYFormat, formatNumber(it)) },
                )
            }
        }

        item { Text(stringResource(R.string.body_history_title), style = MaterialTheme.typography.titleMedium) }

        if (uiState.history.isEmpty()) {
            item { Text(stringResource(R.string.body_history_empty)) }
        } else {
            items(uiState.history, key = { it.id }) { row ->
                HistoryRow(row, onDelete = { onDeleteEntry(row.id) })
            }
        }
    }
}

@Composable
private fun EntryForm(
    sex: BiologicalSex,
    onSexChange: (BiologicalSex) -> Unit,
    onSaveEntry: (Double, Double, Double, Double, Double?) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.body_sex_label), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = sex == BiologicalSex.MALE,
                    onClick = { onSexChange(BiologicalSex.MALE) },
                    label = { Text(stringResource(R.string.body_sex_male)) },
                )
                FilterChip(
                    selected = sex == BiologicalSex.FEMALE,
                    onClick = { onSexChange(BiologicalSex.FEMALE) },
                    label = { Text(stringResource(R.string.body_sex_female)) },
                )
            }
            Spacer(Modifier.height(12.dp))

            NumberField(stringResource(R.string.body_entry_weight_label), weight) { weight = it }
            NumberField(stringResource(R.string.body_entry_height_label), height) { height = it }
            NumberField(stringResource(R.string.body_entry_waist_label), waist) { waist = it }
            NumberField(stringResource(R.string.body_entry_neck_label), neck) { neck = it }
            if (sex == BiologicalSex.FEMALE) {
                NumberField(stringResource(R.string.body_entry_hip_label), hip) { hip = it }
            }

            Spacer(Modifier.height(12.dp))
            val canSave = weight.toDoubleOrNull() != null &&
                height.toDoubleOrNull() != null &&
                waist.toDoubleOrNull() != null &&
                neck.toDoubleOrNull() != null &&
                (sex == BiologicalSex.MALE || hip.toDoubleOrNull() != null)
            Button(
                onClick = {
                    onSaveEntry(
                        weight.toDouble(),
                        height.toDouble(),
                        waist.toDouble(),
                        neck.toDouble(),
                        hip.toDoubleOrNull(),
                    )
                    weight = ""; height = ""; waist = ""; neck = ""; hip = ""
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.body_entry_save)) }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun ResultCard(result: BodyResultUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.body_result_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.body_result_body_fat_format,
                    formatNumber(result.bodyFatPercent),
                    result.category.displayName(),
                ),
            )
            Text(stringResource(R.string.body_result_lean_mass_format, formatNumber(result.leanMassKg)))
            Text(stringResource(R.string.body_result_fat_mass_format, formatNumber(result.fatMassKg)))
            Text(stringResource(R.string.body_result_bmi_format, formatNumber(result.bmi)))
        }
    }
}

@Composable
private fun BodyFatCategory.displayName(): String = stringResource(
    when (this) {
        BodyFatCategory.ESSENTIAL_FAT -> R.string.body_category_essential_fat
        BodyFatCategory.ATHLETES -> R.string.body_category_athletes
        BodyFatCategory.FITNESS -> R.string.body_category_fitness
        BodyFatCategory.ACCEPTABLE -> R.string.body_category_acceptable
        BodyFatCategory.OBESE -> R.string.body_category_obese
    },
)

@Composable
private fun HistoryRow(row: BodyHistoryRow, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(row.date.toString(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    row.bodyFatPct?.let {
                        stringResource(R.string.body_history_row_body_fat_format, formatNumber(row.weightKg), formatNumber(it))
                    } ?: stringResource(R.string.body_history_row_format, formatNumber(row.weightKg)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            TextButton(onClick = { showConfirm = true }) { Text(stringResource(R.string.body_history_delete)) }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.body_history_delete_confirm_title)) },
            text = { Text(stringResource(R.string.body_history_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

private fun formatNumber(value: Double): String = if (value == value.toLong().toDouble()) "${value.toLong()}" else "%.1f".format(value)
