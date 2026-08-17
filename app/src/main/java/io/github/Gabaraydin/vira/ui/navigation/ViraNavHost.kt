package io.github.Gabaraydin.vira.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import io.github.Gabaraydin.vira.ui.placeholder.ComingSoonScreen
import io.github.Gabaraydin.vira.ui.today.TodayRoute

private enum class BottomDestination(val route: String, val labelRes: Int) {
    TODAY("today", R.string.nav_today),
    HISTORY("history", R.string.nav_history),
    EXERCISES("exercises", R.string.nav_exercises),
    BODY("body", R.string.nav_body),
}

private const val ACTIVE_WORKOUT_ARG = "workoutId"
private const val ACTIVE_WORKOUT_ROUTE = "active_workout/{$ACTIVE_WORKOUT_ARG}"

@Composable
fun ViraNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = BottomDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
            composable(BottomDestination.HISTORY.route) { ComingSoonScreen() }
            composable(BottomDestination.EXERCISES.route) { ComingSoonScreen() }
            composable(BottomDestination.BODY.route) { ComingSoonScreen() }
            composable(
                route = ACTIVE_WORKOUT_ROUTE,
                arguments = listOf(navArgument(ACTIVE_WORKOUT_ARG) { type = NavType.LongType }),
            ) { entry ->
                val workoutId = entry.arguments?.getLong(ACTIVE_WORKOUT_ARG) ?: return@composable
                ActiveWorkoutRoute(workoutId = workoutId, onFinished = { navController.popBackStack() })
            }
        }
    }
}
