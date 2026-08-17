package io.github.Gabaraydin.vira.ui.activeworkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.ExerciseRepository
import io.github.Gabaraydin.vira.data.repository.ProgramRepository
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import io.github.Gabaraydin.vira.data.repository.WorkoutRepository
import io.github.Gabaraydin.vira.domain.model.Exercise
import io.github.Gabaraydin.vira.domain.model.ProgramDayExercise
import io.github.Gabaraydin.vira.domain.model.WorkoutSet
import io.github.Gabaraydin.vira.domain.model.displayName
import io.github.Gabaraydin.vira.service.resttimer.RestTimerController
import io.github.Gabaraydin.vira.service.resttimer.RestTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val restTimerController: RestTimerController,
) : ViewModel() {

    val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    val restTimerState: StateFlow<RestTimerState?> = restTimerController.state

    private val previousSetsCache = mutableMapOf<Long, List<PreviousSetSummary>>()
    private val plannedExercises = MutableStateFlow<List<ProgramDayExercise>?>(null)
    private var dayName: String = ""
    private var startedAt: Long = 0
    private var defaultRestSeconds: Int = 90

    // A weight/reps field only commits on blur; tapping Finish directly out of a still-focused
    // field triggers that blur but doesn't wait for it, so a just-typed value could otherwise
    // be silently dropped from the very session summary being computed right after. Tracking
    // the most recently launched edit here lets finishSession() await it before proceeding.
    private var pendingSetUpdateJob: Job? = null

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        workoutRepository.observeSetsForWorkout(workoutId),
        plannedExercises.filterNotNull(),
        settingsRepository.settings,
        exerciseRepository.observeAll(),
    ) { sets, planned, settings, exercises ->
        defaultRestSeconds = settings.defaultRestSeconds
        RawState(sets, planned, settings.rpeEnabled, settings.keepScreenOnDuringSession, exercises)
    }
        .mapLatest { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    init {
        viewModelScope.launch {
            val workout = workoutRepository.getById(workoutId)
            dayName = workout?.dayNameSnapshot.orEmpty()
            startedAt = workout?.startedAt ?: 0
            val programDayId = workout?.programDayId
            plannedExercises.value = if (programDayId != null) {
                programRepository.observeExercisesForDay(programDayId).first()
            } else {
                emptyList()
            }
        }
    }

    private suspend fun RawState.toUiState(): ActiveWorkoutUiState {
        val exercisesById = exercises.associateBy { it.id }
        val plannedSorted = planned.sortedBy { it.position }
        val labels = supersetLabels(plannedSorted)
        val plannedByExerciseId = plannedSorted.associateBy { it.exerciseId }

        val setsByExercise = sets.groupBy { it.exerciseId }
        val plannedIds = plannedSorted.map { it.exerciseId }
        val adHocIds = setsByExercise.keys
            .filter { it !in plannedIds }
            .sortedBy { id -> setsByExercise.getValue(id).minOf { it.position } }

        val exerciseModels = (plannedIds + adHocIds).map { exerciseId ->
            val previous = previousSetsCache.getOrPut(exerciseId) {
                workoutRepository.getPreviousSessionSets(exerciseId, workoutId)
                    .map { PreviousSetSummary(it.weightKg, it.reps, it.isWarmup) }
            }
            ActiveExerciseUiModel(
                exerciseId = exerciseId,
                exerciseName = exercisesById[exerciseId]?.displayName() ?: "?",
                supersetLabel = plannedByExerciseId[exerciseId]?.let { labels[it.id] },
                previousSets = previous,
                sets = setsByExercise[exerciseId].orEmpty().sortedBy { it.setIndex }.map { it.toUiModel() },
            )
        }

        return ActiveWorkoutUiState(
            isLoading = false,
            dayName = dayName,
            startedAt = startedAt,
            rpeEnabled = rpeEnabled,
            keepScreenOn = keepScreenOn,
            exercises = exerciseModels,
        )
    }

    fun addSet(exerciseId: Long) {
        val exercise = uiState.value.exercises.firstOrNull { it.exerciseId == exerciseId } ?: return
        val nextSetIndex = (exercise.sets.maxOfOrNull { it.setIndex } ?: 0) + 1
        val nextPosition = (uiState.value.exercises.flatMap { it.sets }.maxOfOrNull { it.position } ?: -1) + 1
        // Sibling sets are empty for an exercise's very first set, so fall back to the plan's
        // own grouping rather than reading it off a set that doesn't exist yet.
        val supersetGroupId = exercise.sets.firstOrNull()?.supersetGroupId
            ?: plannedExercises.value.orEmpty().firstOrNull { it.exerciseId == exerciseId }?.supersetGroupId
        viewModelScope.launch {
            workoutRepository.addSet(
                WorkoutSet(
                    id = 0, workoutId = workoutId, exerciseId = exerciseId, position = nextPosition,
                    setIndex = nextSetIndex, weightKg = 0.0, reps = 0, rpe = null,
                    isWarmup = false, isCompleted = false, completedAt = null, supersetGroupId = supersetGroupId,
                ),
            )
        }
    }

    fun deleteSet(model: ActiveSetUiModel) {
        viewModelScope.launch { workoutRepository.deleteSet(model.toDomain(workoutId)) }
    }

    fun updateWeight(model: ActiveSetUiModel, weightKg: Double) = update(model) { it.copy(weightKg = weightKg) }

    fun updateReps(model: ActiveSetUiModel, reps: Int) = update(model) { it.copy(reps = reps) }

    fun updateRpe(model: ActiveSetUiModel, rpe: Double?) = update(model) { it.copy(rpe = rpe) }

    fun toggleWarmup(model: ActiveSetUiModel) = update(model) { it.copy(isWarmup = !it.isWarmup) }

    fun toggleCompleted(model: ActiveSetUiModel) {
        val nowCompleted = !model.isCompleted
        val updated = model.toDomain(workoutId)
            .copy(isCompleted = nowCompleted, completedAt = if (nowCompleted) System.currentTimeMillis() else null)
        // Warm-up sets don't count as "working sets" for rest purposes, and a superset's rest
        // only starts once every member's set for this round is done — check with the state
        // from *before* the update lands, since the just-completed set itself isn't in it yet.
        val shouldStartRest = nowCompleted && !model.isWarmup && isLastSupersetMemberForRound(model)
        pendingSetUpdateJob = viewModelScope.launch {
            workoutRepository.updateSet(updated)
            if (shouldStartRest) startRestTimer(model)
        }
    }

    fun skipRest() = restTimerController.skip()

    fun adjustRest(deltaSeconds: Int) = restTimerController.adjust(deltaSeconds)

    private fun isLastSupersetMemberForRound(model: ActiveSetUiModel): Boolean {
        val groupId = model.supersetGroupId ?: return true
        val groupExerciseIds = plannedExercises.value.orEmpty()
            .filter { it.supersetGroupId == groupId }
            .map { it.exerciseId }
        val exercisesById = uiState.value.exercises.associateBy { it.exerciseId }
        return groupExerciseIds.all { exerciseId ->
            exerciseId == model.exerciseId ||
                exercisesById[exerciseId]?.sets.orEmpty().any { it.setIndex == model.setIndex && it.isCompleted }
        }
    }

    private fun startRestTimer(model: ActiveSetUiModel) {
        val restSeconds = plannedExercises.value.orEmpty()
            .firstOrNull { it.exerciseId == model.exerciseId }
            ?.restSecOverride
            ?: defaultRestSeconds
        val exerciseName = uiState.value.exercises.firstOrNull { it.exerciseId == model.exerciseId }?.exerciseName.orEmpty()
        restTimerController.start(restSeconds, exerciseName)
    }

    private fun update(model: ActiveSetUiModel, transform: (WorkoutSet) -> WorkoutSet) {
        pendingSetUpdateJob = viewModelScope.launch { workoutRepository.updateSet(transform(model.toDomain(workoutId))) }
    }

    suspend fun finishSession() {
        pendingSetUpdateJob?.join()
        workoutRepository.finishSession(workoutId, finishedAt = System.currentTimeMillis())
    }
}

private data class RawState(
    val sets: List<WorkoutSet>,
    val planned: List<ProgramDayExercise>,
    val rpeEnabled: Boolean,
    val keepScreenOn: Boolean,
    val exercises: List<Exercise>,
)

private fun WorkoutSet.toUiModel() = ActiveSetUiModel(
    id = id,
    exerciseId = exerciseId,
    position = position,
    setIndex = setIndex,
    supersetGroupId = supersetGroupId,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isWarmup = isWarmup,
    isCompleted = isCompleted,
    completedAt = completedAt,
)

private fun ActiveSetUiModel.toDomain(workoutId: Long) = WorkoutSet(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isWarmup = isWarmup,
    isCompleted = isCompleted,
    completedAt = completedAt,
    supersetGroupId = supersetGroupId,
)

private fun supersetLabels(sortedEntries: List<ProgramDayExercise>): Map<Long, String> {
    val groupOrder = sortedEntries.mapNotNull { it.supersetGroupId }.distinct()
    val letterByGroup = groupOrder.withIndex().associate { (index, groupId) -> groupId to ('A' + index) }
    return sortedEntries.filter { it.supersetGroupId != null }.associate { entry ->
        entry.id to "${letterByGroup.getValue(entry.supersetGroupId!!)}${entry.supersetOrder}"
    }
}
