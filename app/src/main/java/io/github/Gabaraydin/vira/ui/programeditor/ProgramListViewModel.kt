package io.github.Gabaraydin.vira.ui.programeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import io.github.Gabaraydin.vira.domain.model.Program
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramListViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ProgramListUiState> = programRepository.observeUnarchivedPrograms()
        .map { programs -> ProgramListUiState(programs = programs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramListUiState())

    fun createProgram(name: String) {
        viewModelScope.launch { programRepository.createProgram(name, System.currentTimeMillis()) }
    }

    fun duplicateProgram(program: Program, newName: String) {
        viewModelScope.launch {
            programRepository.duplicateProgram(program.id, newName, System.currentTimeMillis())
        }
    }

    fun archiveProgram(program: Program) {
        viewModelScope.launch { programRepository.archiveProgram(program.id, System.currentTimeMillis()) }
    }

    suspend fun shouldShowSwitchExplanation(): Boolean =
        !settingsRepository.settings.first().hasSeenProgramSwitchExplanation

    fun confirmSetActiveProgram(program: Program) {
        viewModelScope.launch {
            programRepository.setActiveProgram(program.id)
            settingsRepository.markProgramSwitchExplanationSeen()
        }
    }
}
