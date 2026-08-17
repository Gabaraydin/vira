package io.github.Gabaraydin.vira.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.domain.model.Equipment
import io.github.Gabaraydin.vira.domain.model.MuscleGroup

@Composable
fun MuscleGroup.displayName(): String = stringResource(
    when (this) {
        MuscleGroup.CHEST -> R.string.muscle_group_chest
        MuscleGroup.BACK -> R.string.muscle_group_back
        MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_group_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_group_triceps
        MuscleGroup.FOREARMS -> R.string.muscle_group_forearms
        MuscleGroup.QUADS -> R.string.muscle_group_quads
        MuscleGroup.HAMSTRINGS -> R.string.muscle_group_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_group_glutes
        MuscleGroup.CALVES -> R.string.muscle_group_calves
        MuscleGroup.CORE -> R.string.muscle_group_core
        MuscleGroup.CARDIO -> R.string.muscle_group_cardio
        MuscleGroup.FULL_BODY -> R.string.muscle_group_full_body
    },
)

@Composable
fun Equipment.displayName(): String = stringResource(
    when (this) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.BAND -> R.string.equipment_band
        Equipment.OTHER -> R.string.equipment_other
    },
)
