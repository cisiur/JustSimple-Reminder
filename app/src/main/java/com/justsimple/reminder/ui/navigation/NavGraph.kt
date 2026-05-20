package com.justsimple.reminder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.justsimple.reminder.reliability.OemReliabilityGuide
import com.justsimple.reminder.ui.addedit.AddEditReminderScreen
import com.justsimple.reminder.ui.diagnostics.DiagnosticsScreen
import com.justsimple.reminder.ui.diagnostics.ReliabilityScreen
import com.justsimple.reminder.ui.reminders.ReminderListScreen
import com.justsimple.reminder.ui.settings.SettingsScreen

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

private const val PRIVACY_POLICY_URL = "https://justsimplereminder-legal.netlify.app/"

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
            val ctx = androidx.compose.ui.platform.LocalContext.current
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDiagnosticsClick = { navController.navigate(Screen.Diagnostics.route) },
                onPaywallClick = { navController.navigate(Screen.Paywall.route) },
                onPrivacyPolicyClick = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
            )
        }

        composable(Screen.Diagnostics.route) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            DiagnosticsScreen(
                onBack = { navController.popBackStack() },
                onOpenBatterySettings = { reliabilityGuide.openBatteryOptimizationSettings() },
                onOpenAlarmSettings = { reliabilityGuide.openAlarmPermissionSettings() },
                onOpenNotificationSettings = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
                onOpenAppSettings = { reliabilityGuide.openAppSettings() },
                onOpenOtherPermissionsSettings = { reliabilityGuide.openMiuiOtherPermissions() },
                onReliabilityClick = { navController.navigate(Screen.Reliability.route) },
            )
        }

        composable(Screen.Reliability.route) {
            ReliabilityScreen(
                onBack = { navController.popBackStack() },
                onOpenAutostart = { reliabilityGuide.openXiaomiAutostart() },
                onOpenBatterySettings = { reliabilityGuide.openBatteryOptimizationSettings() },
                onOpenLockScreenSettings = { reliabilityGuide.openLockScreenSettings() },
                onOpenAppSettings = { reliabilityGuide.openAppSettings() },
            )
        }

        composable(Screen.Paywall.route) {
            // TODO Module 11
        }
    }
}
