package io.github.Gabaraydin.vira.ui.programeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.domain.model.ProgramDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DayEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val programId: Long = checkNotNull(savedStateHandle["programId"])

    val uiState: StateFlow<DayEditorUiState> = combine(
        programRepository.observeProgram(programId),
        programRepository.observeDaysForProgram(programId),
    ) { program, days ->
        DayEditorUiState(programName = program?.name.orEmpty(), days = days.sortedBy { it.position })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditorUiState())

    fun addDay(name: String) {
        viewModelScope.launch {
            programRepository.addDay(programId, name, isRest = false, libraryCategory = null)
        }
    }

    fun removeDay(day: ProgramDay) {
        viewModelScope.launch { programRepository.removeDay(day) }
    }

    fun toggleRest(day: ProgramDay) {
        viewModelScope.launch { programRepository.updateDay(day.copy(isRest = !day.isRest)) }
    }

    fun renameDay(day: ProgramDay, newName: String) {
        viewModelScope.launch { programRepository.updateDay(day.copy(name = newName)) }
    }

    fun moveUp(day: ProgramDay) = reorder(day, offset = -1)

    fun moveDown(day: ProgramDay) = reorder(day, offset = 1)

    private fun reorder(day: ProgramDay, offset: Int) {
        val days = uiState.value.days
        val index = days.indexOfFirst { it.id == day.id }
        val targetIndex = index + offset
        if (index < 0 || targetIndex !in days.indices) return

        val reordered = days.toMutableList()
        reordered.add(targetIndex, reordered.removeAt(index))
        viewModelScope.launch { programRepository.reorderDays(programId, reordered.map { it.id }) }
    }
}
