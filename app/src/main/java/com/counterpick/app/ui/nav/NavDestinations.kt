package com.counterpick.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(val route: String, val label: String, val icon: ImageVector) {
    DRAFT("draft", "Draft", Icons.Filled.SportsEsports),
    META("meta", "Meta", Icons.Filled.Leaderboard),
    LOOKUP("lookup", "Lookup", Icons.Filled.Search),
    LISTS("lists", "My Lists", Icons.Filled.List),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}
