package com.badsheepy.bafflingvision // Or your common navigation package

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.badsheepy.bafflingvision.AppScreens.Home
import com.badsheepy.bafflingvision.AppScreens.Settings
/**
 *     BafflingDisplay android app
 *
 *     Copyright (C) 2025 Dave.J
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

sealed class AppScreens(val route: String, val title: String, val icon: ImageVector) {
    object Home : AppScreens("home", "Home", Icons.Filled.Home)
    object Settings : AppScreens("settings", "Settings", Icons.Filled.Settings)
    object None: AppScreens("none", "Error", Icons.Filled.Error)
}

val TOP_LEVEL_ROUTES : List<AppScreens> = listOf(Home, Settings)
