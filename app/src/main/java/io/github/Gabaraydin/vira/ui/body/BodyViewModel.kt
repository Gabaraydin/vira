package io.github.Gabaraydin.vira.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.Gabaraydin.vira.data.repository.BodyMeasurementRepository
import io.github.Gabaraydin.vira.data.repository.SettingsRepository
import io.github.Gabaraydin.vira.domain.calculations.bodyFatCategory
import io.github.Gabaraydin.vira.domain.calculations.bodyFatPercentFemale
import io.github.Gabaraydin.vira.domain.calculations.bodyFatPercentMale
import io.github.Gabaraydin.vira.domain.calculations.bodyMassIndex
import io.github.Gabaraydin.vira.domain.calculations.fatMassKg
import io.github.Gabaraydin.vira.domain.calculations.leanMassKg
import io.github.Gabaraydin.vira.domain.charts.ChartPoint
import io.github.Gabaraydin.vira.domain.model.BiologicalSex
import io.github.Gabaraydin.vira.domain.model.BodyMeasurement
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BodyViewModel @Inject constructor(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Side-effect-captured by the combine below so deleteEntry()/saveEntry() — imperative
    // calls, not part of the reactive uiState pipeline — can use the latest values.
    private var measurementsById: Map<Long, BodyMeasurement> = emptyMap()
    private var currentSex: BiologicalSex = BiologicalSex.MALE

    val uiState: StateFlow<BodyUiState> = combine(
        bodyMeasurementRepository.observeAll(),
        settingsRepository.settings,
    ) { measurements, settings ->
        measurementsById = measurements.associateBy { it.id }
        currentSex = settings.biologicalSex
        buildUiState(measurements, settings.biologicalSex)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    fun setSex(sex: BiologicalSex) {
        viewModelScope.launch { settingsRepository.setBiologicalSex(sex) }
    }

    fun saveEntry(weightKg: Double, heightCm: Double, waistCm: Double, neckCm: Double, hipCm: Double?) {
        viewModelScope.launch {
            val bodyFatPct = if (currentSex == BiologicalSex.MALE) {
                bodyFatPercentMale(waistCm = waistCm, neckCm = neckCm, heightCm = heightCm)
            } else {
                bodyFatPercentFemale(waistCm = waistCm, neckCm = neckCm, hipCm = hipCm ?: 0.0, heightCm = heightCm)
            }
            bodyMeasurementRepository.upsert(
                BodyMeasurement(
                    id = 0,
                    date = LocalDate.now(),
                    weightKg = weightKg,
                    heightCm = heightCm,
                    waistCm = waistCm,
                    neckCm = neckCm,
                    hipCm = hipCm,
                    bodyFatPct = bodyFatPct,
                    note = null,
                ),
            )
        }
    }

    fun deleteEntry(id: Long) {
        val measurement = measurementsById[id] ?: return
        viewModelScope.launch { bodyMeasurementRepository.delete(measurement) }
    }
}

private fun buildUiState(measurements: List<BodyMeasurement>, sex: BiologicalSex): BodyUiState {
    val sorted = measurements.sortedBy { it.date }
    val latest = sorted.lastOrNull()

    val latestResult = latest?.bodyFatPct?.let { bodyFatPct ->
        val leanMass = leanMassKg(latest.weightKg, bodyFatPct)
        BodyResultUiModel(
            bodyFatPercent = bodyFatPct,
            category = bodyFatCategory(bodyFatPct, sex),
            leanMassKg = leanMass,
            fatMassKg = fatMassKg(latest.weightKg, leanMass),
            bmi = bodyMassIndex(latest.weightKg, latest.heightCm),
        )
    }

    return BodyUiState(
        isLoading = false,
        sex = sex,
        latestResult = latestResult,
        weightChartPoints = sorted.map { ChartPoint(it.date.toEpochDay().toDouble(), it.weightKg) },
        bodyFatChartPoints = sorted.mapNotNull { m -> m.bodyFatPct?.let { ChartPoint(m.date.toEpochDay().toDouble(), it) } },
        history = sorted.reversed().map { BodyHistoryRow(it.id, it.date, it.weightKg, it.bodyFatPct) },
    )
}
