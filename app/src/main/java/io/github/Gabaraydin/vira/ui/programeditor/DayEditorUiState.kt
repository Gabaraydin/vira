package io.github.Gabaraydin.vira.ui.programeditor

import io.github.Gabaraydin.vira.domain.model.ProgramDay

data class DayEditorUiState(val programName: String = "", val days: List<ProgramDay> = emptyList())
