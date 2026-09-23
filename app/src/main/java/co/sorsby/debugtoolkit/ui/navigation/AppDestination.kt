package co.sorsby.debugtoolkit.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import co.sorsby.debugtoolkit.R

data class AppDestination(
    val route: String,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
)

object AppDestinations {
    val Overview = AppDestination(
        "overview",
        R.string.nav_overview,
        R.string.nav_overview_description,
        Icons.Default.Home,
    )
    val Network = AppDestination(
        "network",
        R.string.nav_network,
        R.string.nav_network_description,
        Icons.Default.Wifi,
    )
    val Tools = AppDestination(
        "tools",
        R.string.nav_tools,
        R.string.nav_tools_description,
        Icons.Default.Build,
    )
    val Speed = AppDestination(
        "speed",
        R.string.nav_speed,
        R.string.nav_speed_description,
        Icons.Default.Speed,
    )
    val Tls = AppDestination(
        "tls",
        R.string.nav_tls,
        R.string.nav_tls_description,
        Icons.Default.Security,
    )
    val Dns = AppDestination(
        "dns",
        R.string.nav_dns,
        R.string.nav_dns_description,
        Icons.Default.Dns,
    )
    val Http = AppDestination(
        "http",
        R.string.nav_http,
        R.string.nav_http_description,
        Icons.Default.Language,
    )
    val Settings = AppDestination(
        "settings",
        R.string.nav_settings,
        R.string.nav_settings_description,
        Icons.Default.Settings,
    )
    val About = AppDestination(
        "about",
        R.string.nav_about,
        R.string.nav_about_description,
        Icons.Default.Info,
    )

    val bottom = listOf(Overview, Network, Tools)
    val diagnostics = listOf(Speed, Tls, Dns, Http)
    val drawer = diagnostics + Settings + About
    val all = bottom + drawer
}
