package io.github.Gabaraydin.vira.ui.programeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.domain.model.ProgramDayExercise
import io.github.Gabaraydin.vira.domain.model.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlannedExercisesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val programDayId: Long = checkNotNull(savedStateHandle["programDayId"])

    private val dayName = MutableStateFlow("")

    val uiState: StateFlow<PlannedExercisesUiState> = combine(
        programRepository.observeExercisesForDay(programDayId),
        exerciseRepository.observeAll(),
        dayName,
    ) { entries, exercises, name ->
        val exercisesById = exercises.associateBy { it.id }
        val sorted = entries.sortedBy { it.position }
        val labels = supersetLabels(sorted)
        PlannedExercisesUiState(
            dayName = name,
            exercises = sorted.map { entry ->
                PlannedExerciseUiModel(
                    entryId = entry.id,
                    exerciseId = entry.exerciseId,
                    exerciseName = exercisesById[entry.exerciseId]?.displayName() ?: "?",
                    position = entry.position,
                    targetSets = entry.targetSets,
                    targetRepsMin = entry.targetRepsMin,
                    targetRepsMax = entry.targetRepsMax,
                    supersetGroupId = entry.supersetGroupId,
                    supersetOrder = entry.supersetOrder,
                    supersetLabel = labels[entry.id],
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannedExercisesUiState())

    init {
        viewModelScope.launch { dayName.value = programRepository.getDay(programDayId)?.name.orEmpty() }
    }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            programRepository.addExerciseToDay(
                programDayId, exerciseId, targetSets = 3,
                targetRepsMin = null, targetRepsMax = null, targetWeightKg = null, restSecOverride = null,
            )
        }
    }

    fun removeExercise(entryId: Long) {
        viewModelScope.launch { programRepository.removeExerciseFromDay(entryId) }
    }

    fun updateSets(model: PlannedExerciseUiModel, sets: Int) {
        viewModelScope.launch { programRepository.updateExerciseTarget(model.toEntry(targetSets = sets)) }
    }

    fun updateRepsMin(model: PlannedExerciseUiModel, repsMin: Int?) {
        viewModelScope.launch { programRepository.updateExerciseTarget(model.toEntry(targetRepsMin = repsMin)) }
    }

    fun updateRepsMax(model: PlannedExerciseUiModel, repsMax: Int?) {
        viewModelScope.launch { programRepository.updateExerciseTarget(model.toEntry(targetRepsMax = repsMax)) }
    }

    fun groupIntoSuperset(entryIds: List<Long>) {
        viewModelScope.launch { programRepository.groupIntoSuperset(programDayId, entryIds) }
    }

    fun ungroupSuperset(entryIds: List<Long>) {
        viewModelScope.launch { programRepository.ungroupSuperset(entryIds) }
    }

    // Grouped exercises can't be reordered without first ungrouping (see ProgramRepository);
    // this keeps that rule visible to the UI so it can disable the buttons.
    fun moveUp(model: PlannedExerciseUiModel) = reorder(model, offset = -1)

    fun moveDown(model: PlannedExerciseUiModel) = reorder(model, offset = 1)

    private fun reorder(model: PlannedExerciseUiModel, offset: Int) {
        if (model.supersetGroupId != null) return
        val entries = uiState.value.exercises
        val index = entries.indexOfFirst { it.entryId == model.entryId }
        val targetIndex = index + offset
        if (index < 0 || targetIndex !in entries.indices) return

        val reordered = entries.toMutableList()
        reordered.add(targetIndex, reordered.removeAt(index))
        viewModelScope.launch {
            programRepository.reorderExercisesInDay(programDayId, reordered.map { it.entryId })
        }
    }

    private fun PlannedExerciseUiModel.toEntry(
        targetSets: Int = this.targetSets,
        targetRepsMin: Int? = this.targetRepsMin,
        targetRepsMax: Int? = this.targetRepsMax,
    ) = ProgramDayExercise(
        id = entryId,
        programDayId = programDayId,
        exerciseId = exerciseId,
        position = position,
        supersetGroupId = supersetGroupId,
        supersetOrder = supersetOrder,
        targetSets = targetSets,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        targetWeightKg = null,
        restSecOverride = null,
    )
}

private fun supersetLabels(sortedEntries: List<ProgramDayExercise>): Map<Long, String> {
    val groupOrder = sortedEntries.mapNotNull { it.supersetGroupId }.distinct()
    val letterByGroup = groupOrder.withIndex().associate { (index, groupId) -> groupId to ('A' + index) }
    return sortedEntries.filter { it.supersetGroupId != null }.associate { entry ->
        entry.id to "${letterByGroup.getValue(entry.supersetGroupId!!)}${entry.supersetOrder}"
    }
}
