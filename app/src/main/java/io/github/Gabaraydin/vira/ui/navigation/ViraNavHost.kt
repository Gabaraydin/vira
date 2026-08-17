package io.github.Gabaraydin.vira.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.Gabaraydin.vira.R
import io.github.Gabaraydin.vira.ui.activeworkout.ActiveWorkoutRoute
import io.github.Gabaraydin.vira.ui.ads.BannerAdView
import io.github.Gabaraydin.vira.ui.exercisedetail.ExerciseDetailRoute
import io.github.Gabaraydin.vira.ui.exercisepicker.ExercisePickerRoute
import io.github.Gabaraydin.vira.ui.exerciselibrary.ExerciseLibraryRoute
import io.github.Gabaraydin.vira.ui.placeholder.ComingSoonScreen
import io.github.Gabaraydin.vira.ui.programeditor.DayEditorRoute
import io.github.Gabaraydin.vira.ui.programeditor.PlannedExercisesRoute
import io.github.Gabaraydin.vira.ui.history.HistoryRoute
import io.github.Gabaraydin.vira.ui.programeditor.ProgramListRoute
import io.github.Gabaraydin.vira.ui.today.TodayRoute
import io.github.Gabaraydin.vira.ui.workoutdetail.WorkoutDetailRoute
import io.github.Gabaraydin.vira.ui.workoutsummary.WorkoutSummaryRoute

private enum class BottomDestination(val route: String, val labelRes: Int) {
    TODAY("today", R.string.nav_today),
    HISTORY("history", R.string.nav_history),
    EXERCISES("exercises", R.string.nav_exercises),
    BODY("body", R.string.nav_body),
}

private const val ACTIVE_WORKOUT_ARG = "workoutId"
private const val ACTIVE_WORKOUT_ROUTE = "active_workout/{$ACTIVE_WORKOUT_ARG}"
private const val WORKOUT_SUMMARY_ROUTE = "workout_summary/{$ACTIVE_WORKOUT_ARG}"
private const val WORKOUT_DETAIL_ROUTE = "workout_detail/{$ACTIVE_WORKOUT_ARG}"
private const val PROGRAM_LIST_ROUTE = "program_list"
private const val SETTINGS_ROUTE = "settings"
private const val DAY_EDITOR_ARG = "programId"
private const val DAY_EDITOR_ROUTE = "day_editor/{$DAY_EDITOR_ARG}"
private const val DAY_EXERCISES_ARG = "programDayId"
private const val DAY_EXERCISES_ROUTE = "day_exercises/{$DAY_EXERCISES_ARG}"
private const val EXERCISE_PICKER_ROUTE = "exercise_picker/{$DAY_EXERCISES_ARG}"
private const val EXERCISE_PICKER_LOG_ROUTE = "exercise_picker_log/{$ACTIVE_WORKOUT_ARG}"
private const val EXERCISE_DETAIL_ARG = "exerciseId"
private const val EXERCISE_DETAIL_ROUTE = "exercise_detail/{$EXERCISE_DETAIL_ARG}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViraNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = BottomDestination.entries.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {},
                    actions = {
                        TextButton(onClick = { navController.navigate(PROGRAM_LIST_ROUTE) }) {
                            Text(stringResource(R.string.top_bar_program))
                        }
                        TextButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) {
                            Text(stringResource(R.string.top_bar_settings))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // Persistent banner, never interstitial/fullscreen — shown only on the
                    // 4 main tabs, never during an active workout or any other focused flow.
                    BannerAdView()
                    NavigationBar {
                        BottomDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(BottomDestination.TODAY.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {},
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomDestination.TODAY.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(BottomDestination.TODAY.route) {
                TodayRoute(onNavigateToActiveWorkout = { id -> navController.navigate("active_workout/$id") })
            }
            composable(BottomDestination.HISTORY.route) {
                HistoryRoute(onOpenWorkout = { id -> navController.navigate("workout_detail/$id") })
            }
            composable(BottomDestination.EXERCISES.route) {
                ExerciseLibraryRoute(onOpenExercise = { id -> navController.navigate("exercise_detail/$id") })
            }
            composable(
                route = EXERCISE_DETAIL_ROUTE,
                arguments = listOf(navArgument(EXERCISE_DETAIL_ARG) { type = NavType.LongType }),
            ) {
                ExerciseDetailRoute(onArchived = { navController.popBackStack() })
            }
            composable(BottomDestination.BODY.route) { ComingSoonScreen() }
            composable(SETTINGS_ROUTE) { ComingSoonScreen() }
            composable(
                route = ACTIVE_WORKOUT_ROUTE,
                arguments = listOf(navArgument(ACTIVE_WORKOUT_ARG) { type = NavType.LongType }),
            ) { entry ->
                val workoutId = entry.arguments?.getLong(ACTIVE_WORKOUT_ARG) ?: return@composable
                ActiveWorkoutRoute(
                    workoutId = workoutId,
                    onAddExercise = { id -> navController.navigate("exercise_picker_log/$id") },
                    onFinished = {
                        navController.navigate("workout_summary/$workoutId") {
                            popUpTo(ACTIVE_WORKOUT_ROUTE) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = WORKOUT_SUMMARY_ROUTE,
                arguments = listOf(navArgument(ACTIVE_WORKOUT_ARG) { type = NavType.LongType }),
            ) {
                WorkoutSummaryRoute(onDone = { navController.popBackStack() })
            }
            composable(
                route = WORKOUT_DETAIL_ROUTE,
                arguments = listOf(navArgument(ACTIVE_WORKOUT_ARG) { type = NavType.LongType }),
            ) {
                WorkoutDetailRoute(onDeleted = { navController.popBackStack() })
            }
            composable(PROGRAM_LIST_ROUTE) {
                ProgramListRoute(onOpenDayEditor = { programId -> navController.navigate("day_editor/$programId") })
            }
            composable(
                route = DAY_EDITOR_ROUTE,
                arguments = listOf(navArgument(DAY_EDITOR_ARG) { type = NavType.LongType }),
            ) {
                DayEditorRoute(onOpenExercises = { dayId -> navController.navigate("day_exercises/$dayId") })
            }
            composable(
                route = DAY_EXERCISES_ROUTE,
                arguments = listOf(navArgument(DAY_EXERCISES_ARG) { type = NavType.LongType }),
            ) {
                PlannedExercisesRoute(onAddExercise = { dayId -> navController.navigate("exercise_picker/$dayId") })
            }
            composable(
                route = EXERCISE_PICKER_ROUTE,
                arguments = listOf(navArgument(DAY_EXERCISES_ARG) { type = NavType.LongType }),
            ) {
                ExercisePickerRoute(onDone = { navController.popBackStack() })
            }
            composable(
                route = EXERCISE_PICKER_LOG_ROUTE,
                arguments = listOf(navArgument(ACTIVE_WORKOUT_ARG) { type = NavType.LongType }),
            ) {
                ExercisePickerRoute(onDone = { navController.popBackStack() })
            }
        }
    }
}
