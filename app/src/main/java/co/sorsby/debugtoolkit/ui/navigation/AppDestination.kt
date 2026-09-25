package co.sorsby.debugtoolkit.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import co.sorsby.debugtoolkit.R

/**
 * Groups the drawer's diagnostic tools under a labelled heading so the list stays scannable
 * as more tools are added. Destinations outside these groups (overview, settings, about) are
 * not associated with a section.
 */
enum class DiagnosticsSection(@StringRes val label: Int) {
    CONNECTIVITY(R.string.nav_section_connectivity),
    SECURITY(R.string.nav_section_security),
    DISCOVERY(R.string.nav_section_discovery),
}

data class AppDestination(
    val route: String,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
    val section: DiagnosticsSection? = null,
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
        DiagnosticsSection.CONNECTIVITY,
    )
    val Ping = AppDestination(
        "ping",
        R.string.nav_ping,
        R.string.nav_ping_description,
        Icons.Default.NetworkPing,
        DiagnosticsSection.CONNECTIVITY,
    )
    val PublicIp = AppDestination(
        "publicip",
        R.string.nav_publicip,
        R.string.nav_publicip_description,
        Icons.Default.Public,
        DiagnosticsSection.CONNECTIVITY,
    )
    val Tls = AppDestination(
        "tls",
        R.string.nav_tls,
        R.string.nav_tls_description,
        Icons.Default.Security,
        DiagnosticsSection.SECURITY,
    )
    val PortScanner = AppDestination(
        "portscan",
        R.string.nav_portscan,
        R.string.nav_portscan_description,
        Icons.Default.Radar,
        DiagnosticsSection.SECURITY,
    )
    val Dns = AppDestination(
        "dns",
        R.string.nav_dns,
        R.string.nav_dns_description,
        Icons.Default.Dns,
        DiagnosticsSection.DISCOVERY,
    )
    val Whois = AppDestination(
        "whois",
        R.string.nav_whois,
        R.string.nav_whois_description,
        Icons.Default.Domain,
        DiagnosticsSection.DISCOVERY,
    )
    val Http = AppDestination(
        "http",
        R.string.nav_http,
        R.string.nav_http_description,
        Icons.Default.Language,
        DiagnosticsSection.DISCOVERY,
    )
    val LanScanner = AppDestination(
        "lanscan",
        R.string.nav_lanscan,
        R.string.nav_lanscan_description,
        Icons.Default.DeviceHub,
        DiagnosticsSection.DISCOVERY,
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

    /** Every diagnostic tool, ordered within its section. Used by the Overview and Tools screens. */
    val diagnostics = listOf(Speed, Ping, PublicIp, Tls, PortScanner, Dns, Whois, Http, LanScanner)

    /** Diagnostic tools grouped by section, in display order, for the drawer and Tools screen. */
    val diagnosticsBySection: List<Pair<DiagnosticsSection, List<AppDestination>>> =
        DiagnosticsSection.entries.map { section ->
            section to diagnostics.filter { it.section == section }
        }

    val drawer = diagnostics + Settings + About
    val all = bottom + drawer
}
