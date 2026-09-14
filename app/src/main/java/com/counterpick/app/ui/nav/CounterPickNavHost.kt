package com.counterpick.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.counterpick.app.ui.draft.DraftScreen
import com.counterpick.app.ui.lists.ListsScreen
import com.counterpick.app.ui.lookup.LookupScreen
import com.counterpick.app.ui.meta.MetaScreen
import com.counterpick.app.ui.settings.SettingsScreen

@Composable
fun CounterPickNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination

                NavDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true,
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.DRAFT.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(NavDestination.DRAFT.route) { DraftScreen() }
            composable(NavDestination.META.route) { MetaScreen() }
            composable(NavDestination.LOOKUP.route) { LookupScreen() }
            composable(NavDestination.LISTS.route) { ListsScreen() }
            composable(NavDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
