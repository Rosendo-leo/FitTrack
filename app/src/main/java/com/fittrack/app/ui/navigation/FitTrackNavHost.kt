package com.fittrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fittrack.app.ui.screens.active_session.ActiveSessionScreen
import com.fittrack.app.ui.screens.dashboard.DashboardScreen
import com.fittrack.app.ui.screens.history.HistoryScreen
import com.fittrack.app.ui.screens.history.SessionDetailScreen
import com.fittrack.app.ui.screens.progress.ProgressScreen
import com.fittrack.app.ui.screens.settings.SettingsScreen
import com.fittrack.app.ui.screens.workout.WorkoutsScreen
import com.fittrack.app.ui.screens.workout.WorkoutsViewModel
import com.fittrack.app.ui.screens.workout.editor.WorkoutEditorScreen

const val WORKOUT_EDITOR_ROUTE = "workout_editor/{templateId}"
const val ACTIVE_SESSION_ROUTE = "active_session/{sessionId}"
const val SESSION_DETAIL_ROUTE = "session_detail/{sessionId}"

fun workoutEditorRoute(templateId: Long) = "workout_editor/$templateId"
fun activeSessionRoute(sessionId: Long) = "active_session/$sessionId"
fun sessionDetailRoute(sessionId: Long) = "session_detail/$sessionId"

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Destination("dashboard", "Início", Icons.Default.Home)
    data object Workouts : Destination("workouts", "Treinos", Icons.Default.FitnessCenter)
    data object Progress : Destination("progress", "Progresso", Icons.Default.TrendingUp)
    data object History : Destination("history", "Histórico", Icons.Default.CalendarMonth)
    data object Settings : Destination("settings", "Ajustes", Icons.Default.Settings)

    companion object {
        val bottomNav = listOf(Dashboard, Workouts, Progress, History, Settings)
    }
}

@Composable
fun FitTrackNavHost(
    widgetAction: WidgetAction? = null,
    onWidgetActionHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var autoOpenWeightDialog by rememberSaveable { mutableStateOf(false) }
    val workoutsViewModel: WorkoutsViewModel = hiltViewModel()

    LaunchedEffect(widgetAction) {
        when (widgetAction) {
            is WidgetAction.RegisterWeight -> {
                autoOpenWeightDialog = true
                navController.navigate(Destination.Dashboard.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                }
                onWidgetActionHandled()
            }
            is WidgetAction.StartWorkout -> {
                val templateId = widgetAction.templateId
                if (templateId != null) {
                    workoutsViewModel.startSession(templateId) { sessionId ->
                        navController.navigate(activeSessionRoute(sessionId))
                    }
                } else {
                    navController.navigate(Destination.Workouts.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                }
                onWidgetActionHandled()
            }
            is WidgetAction.OpenActiveSession -> {
                navController.navigate(activeSessionRoute(widgetAction.sessionId))
                onWidgetActionHandled()
            }
            null -> Unit
        }
    }

    val showBottomBar = Destination.bottomNav.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            NavigationBar {
                Destination.bottomNav.forEach { dest ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Dashboard.route) {
                DashboardScreen(
                    onOpenSession = { sessionId ->
                        navController.navigate(activeSessionRoute(sessionId))
                    },
                    autoOpenWeightDialog = autoOpenWeightDialog,
                    onAutoOpenHandled = { autoOpenWeightDialog = false }
                )
            }
            composable(Destination.Workouts.route) {
                WorkoutsScreen(
                    onOpenEditor = { templateId ->
                        navController.navigate(workoutEditorRoute(templateId))
                    },
                    onOpenSession = { sessionId ->
                        navController.navigate(activeSessionRoute(sessionId))
                    }
                )
            }
            composable(Destination.Progress.route) { ProgressScreen() }
            composable(Destination.History.route) {
                HistoryScreen(
                    onOpenSession = { sessionId ->
                        navController.navigate(sessionDetailRoute(sessionId))
                    }
                )
            }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(
                route = WORKOUT_EDITOR_ROUTE,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) {
                WorkoutEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = ACTIVE_SESSION_ROUTE,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) {
                ActiveSessionScreen(onExit = { navController.popBackStack() })
            }
            composable(
                route = SESSION_DETAIL_ROUTE,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) {
                SessionDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
