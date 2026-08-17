package io.github.Gabaraydin.vira.ui.exercisepicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.domain.model.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val programDayId: Long = checkNotNull(savedStateHandle["programDayId"])

    private val query = MutableStateFlow("")
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        exerciseRepository.observeActive(),
        query,
        selectedIds,
    ) { exercises, currentQuery, selected ->
        val filtered = if (currentQuery.isBlank()) {
            exercises
        } else {
            exercises.filter {
                it.nameEn.contains(currentQuery, ignoreCase = true) || it.nameTr.contains(currentQuery, ignoreCase = true)
            }
        }
        ExercisePickerUiState(
            query = currentQuery,
            exercises = filtered.sortedBy { it.displayName() },
            selectedIds = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun setQuery(newQuery: String) {
        query.value = newQuery
    }

    fun toggleSelected(exerciseId: Long) {
        selectedIds.value = selectedIds.value.let { if (exerciseId in it) it - exerciseId else it + exerciseId }
    }

    fun confirmSelection(onDone: () -> Unit) {
        val ids = selectedIds.value
        viewModelScope.launch {
            ids.forEach { exerciseId ->
                programRepository.addExerciseToDay(
                    programDayId, exerciseId, targetSets = 3,
                    targetRepsMin = null, targetRepsMax = null, targetWeightKg = null, restSecOverride = null,
                )
            }
            onDone()
        }
    }
}
