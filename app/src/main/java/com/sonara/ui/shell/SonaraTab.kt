package com.sonara.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level destinations of the Sonara shell. */
enum class SonaraTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Search("Search", Icons.Filled.Search, Icons.Outlined.Search),
    Library("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    Flow("Flow", Icons.Filled.Waves, Icons.Outlined.Waves),
}
