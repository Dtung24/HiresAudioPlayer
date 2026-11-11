package com.hiresaudioplayer.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiresaudioplayer.app.ui.theme.*

enum class AppDestination(val label: String, val icon: ImageVector) {
    NOW_PLAYING("Playing", Icons.Default.PlayCircle),
    LIBRARY("Library", Icons.Default.QueueMusic),
    EQUALIZER("Equalizer", Icons.Default.GraphicEq),
    DSP_SETTINGS("192k DSP", Icons.Default.Tune),
    GOOGLE_DRIVE("Drive", Icons.Default.CloudDownload)
}

@Composable
fun AppBottomNavigation(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        AppDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        fontSize = 11.sp,
                        color = if (isSelected) GoldAccent else TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoldAccent,
                    unselectedIconColor = TextSecondary,
                    indicatorColor = SurfaceDarkVariant
                )
            )
        }
    }
}

