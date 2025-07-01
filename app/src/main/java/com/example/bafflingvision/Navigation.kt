package com.example.bafflingvision // Or your common navigation package

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.bafflingvision.AppScreens.Home
import com.example.bafflingvision.AppScreens.Settings

sealed class AppScreens(val route: String, val title: String, val icon: ImageVector) {
    object Home : AppScreens("home", "Home", Icons.Filled.Home)
    object Settings : AppScreens("settings", "Settings", Icons.Filled.Settings)
    object None: AppScreens("none", "Error", Icons.Filled.Error)
}

val TOP_LEVEL_ROUTES : List<AppScreens> = listOf(Home, Settings)
