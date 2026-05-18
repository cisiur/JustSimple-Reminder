package com.justsimple.reminder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

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
fun JustSimpleReminderNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.ReminderList.route) {
        // TODO Module 6-10: add composable() destinations
    }
}

