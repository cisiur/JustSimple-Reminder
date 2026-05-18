package com.justsimple.reminder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.justsimple.reminder.reliability.OemReliabilityGuide
import com.justsimple.reminder.ui.addedit.AddEditReminderScreen
import com.justsimple.reminder.ui.reminders.ReminderListScreen

sealed class Screen(val route: String) {
    data object ReminderList : Screen("reminders")
    data object AddReminder : Screen("reminders/add")
    data class EditReminder(val id: Long = 0L) : Screen("reminders/edit/{reminderId}") {
        fun routeWithId(id: Long) = "reminders/edit/$id"
        companion object { const val ARG = "reminderId" }
    }
    data object Settings : Screen("settings")
    data object Diagnostics : Screen("diagnostics")
    data object Reliability : Screen("reliability")
    data object Paywall : Screen("paywall")
}

@Composable
fun JustSimpleReminderNavHost(
    reliabilityGuide: OemReliabilityGuide,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Screen.ReminderList.route) {

        composable(Screen.ReminderList.route) {
            ReminderListScreen(
                onAddClick = { navController.navigate(Screen.AddReminder.route) },
                onEditClick = { id -> navController.navigate(Screen.EditReminder().routeWithId(id)) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onOpenBatterySettings = { reliabilityGuide.openBatteryOptimizationSettings() },
                onOpenAlarmSettings = { reliabilityGuide.openAlarmPermissionSettings() },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
            )
        }

        composable(
            route = Screen.EditReminder().route,
            arguments = listOf(navArgument(Screen.EditReminder.ARG) { type = NavType.LongType }),
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong(Screen.EditReminder.ARG)
            AddEditReminderScreen(
                reminderId = reminderId,
                onBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
            )
        }

        composable(Screen.AddReminder.route) {
            AddEditReminderScreen(
                reminderId = null,
                onBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
            )
        }

        composable(Screen.Settings.route) {
            // TODO Module 9
        }

        composable(Screen.Diagnostics.route) {
            // TODO Module 10
        }

        composable(Screen.Reliability.route) {
            // TODO Module 10
        }

        composable(Screen.Paywall.route) {
            // TODO Module 11
        }
    }
}
