package io.github.Gabaraydin.vira.ui.exerciselibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.Exercise
import io.github.Gabaraydin.vira.domain.model.MuscleGroup
import io.github.Gabaraydin.vira.domain.model.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val muscleFilter = MutableStateFlow<MuscleGroup?>(null)
    private val equipmentFilter = MutableStateFlow<Equipment?>(null)
    private val customOnly = MutableStateFlow(false)

    private val filters = combine(query, muscleFilter, equipmentFilter, customOnly, ::Filters)

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        exerciseRepository.observeActive(),
        exerciseRepository.observeLastPerformedDates(),
        filters,
    ) { exercises, lastPerformed, f ->
        val filtered = exercises.filter { ex ->
            (f.muscle == null || ex.primaryMuscle == f.muscle) &&
                (f.equipment == null || ex.equipment == f.equipment) &&
                (!f.customOnly || ex.isCustom) &&
                (f.query.isBlank() || ex.nameEn.contains(f.query, ignoreCase = true) || ex.nameTr.contains(f.query, ignoreCase = true))
        }
        ExerciseLibraryUiState(
            isLoading = false,
            query = f.query,
            muscleFilter = f.muscle,
            equipmentFilter = f.equipment,
            customOnly = f.customOnly,
            exercises = filtered.sortedBy { it.displayName() }.map { it.toRow(lastPerformed[it.id]) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setMuscleFilter(value: MuscleGroup?) {
        muscleFilter.value = if (muscleFilter.value == value) null else value
    }

    fun setEquipmentFilter(value: Equipment?) {
        equipmentFilter.value = if (equipmentFilter.value == value) null else value
    }

    fun setCustomOnly(value: Boolean) {
        customOnly.value = value
    }

    fun addCustom(name: String, muscle: MuscleGroup, equipment: Equipment, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = exerciseRepository.addCustom(name, muscle, equipment)
            onDone(id)
        }
    }
}

private data class Filters(val query: String, val muscle: MuscleGroup?, val equipment: Equipment?, val customOnly: Boolean)

private fun Exercise.toRow(lastPerformed: LocalDate?) = ExerciseRowUiModel(
    id = id,
    name = displayName(),
    muscle = primaryMuscle,
    equipment = equipment,
    lastPerformed = lastPerformed,
)
