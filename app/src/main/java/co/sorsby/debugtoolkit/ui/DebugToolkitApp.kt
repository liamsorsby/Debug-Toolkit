package co.sorsby.debugtoolkit.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.data.dns.DnsRecordType
import co.sorsby.debugtoolkit.data.http.HttpMethod
import co.sorsby.debugtoolkit.feature.DnsViewModel
import co.sorsby.debugtoolkit.feature.HttpViewModel
import co.sorsby.debugtoolkit.feature.NetworkViewModel
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.feature.SpeedViewModel
import co.sorsby.debugtoolkit.feature.TlsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    Destination("overview", "Overview", Icons.Default.Home),
    Destination("network", "Network", Icons.Default.Wifi),
    Destination("tools", "Tools", Icons.Default.Build),
)

private val drawerDestinations = listOf(
    Destination("speed", "Speed test", Icons.Default.Speed),
    Destination("tls", "Certificate inspector", Icons.Default.Info),
    Destination("dns", "DNS lookup", Icons.Default.NetworkCheck),
    Destination("http", "HTTP inspector", Icons.Default.NetworkCheck),
    Destination("settings", "Settings", Icons.Default.Settings),
    Destination("about", "About", Icons.Default.Info),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolkitApp(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        androidx.compose.material3.DrawerValue.Closed,
    )
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "overview"
    val title = (bottomDestinations + drawerDestinations)
        .firstOrNull { it.route == route }?.label ?: "Debug Toolkit"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Debug Toolkit",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp),
                )
                drawerDestinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        selected = route == destination.route,
                        icon = { Icon(destination.icon, contentDescription = null) },
                        onClick = {
                            navController.navigateSingle(destination.route)
                            scope.launch { drawerState.close() }
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = { navController.navigateSingle(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { padding ->
            AppNavHost(navController, padding, settingsViewModel)
        }
    }
}

private fun NavHostController.navigateSingle(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) { saveState = true }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    padding: PaddingValues,
    settingsViewModel: SettingsViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = "overview",
        modifier = Modifier.padding(padding),
    ) {
        composable("overview") { OverviewScreen(navController) }
        composable("network") { NetworkScreen() }
        composable("tools") { ToolsScreen(navController) }
        composable("speed") { SpeedScreen(settingsViewModel = settingsViewModel) }
        composable("tls") { TlsScreen() }
        composable("dns") { DnsScreen() }
        composable("http") { HttpScreen() }
        composable("settings") { SettingsScreen(settingsViewModel) }
        composable("about") { AboutScreen() }
    }
}

@Composable
private fun OverviewScreen(navController: NavHostController) {
    ScreenList {
        item {
            Text("Network diagnostics in one place", style = MaterialTheme.typography.headlineSmall)
            Text("Inspect your connection and run focused checks only when you request them.")
        }
        items(drawerDestinations.take(4)) { destination ->
            ToolCard(destination.label) { navController.navigateSingle(destination.route) }
        }
    }
}

@Composable
private fun ToolsScreen(navController: NavHostController) {
    ScreenList {
        items(drawerDestinations.take(4)) { destination ->
            ToolCard(destination.label) { navController.navigateSingle(destination.route) }
        }
    }
}

@Composable
private fun ToolCard(label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun NetworkScreen(viewModel: NetworkViewModel = koinViewModel()) {
    val context = LocalContext.current
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
    var permissionGranted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants -> permissionGranted = permissions.all { grants[it] == true } }
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()

    ScreenList {
        item {
            if (!permissionGranted) {
                InfoCard("Wi-Fi signal permission is needed only to display RSSI in dBm.")
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Allow Wi-Fi signal access")
                }
            }
            NetworkDetails(snapshot)
        }
    }
}

@Composable
private fun NetworkDetails(snapshot: NetworkSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Metric("Connection", if (snapshot.connected) "Connected" else "Offline")
        Metric("Internet validated", snapshot.validated.yesNo())
        Metric("Transport", snapshot.transports.joinToString().ifEmpty { "Unavailable" })
        Metric("Metered", snapshot.metered.yesNo())
        Metric("Wi-Fi signal", snapshot.wifiRssiDbm?.let { "$it dBm (${snapshot.wifiSignal})" }
            ?: "Permission required or unavailable")
        Metric("Estimated downstream", snapshot.linkDownKbps?.let { "$it Kbps" } ?: "Unavailable")
        Metric("Estimated upstream", snapshot.linkUpKbps?.let { "$it Kbps" } ?: "Unavailable")
        Metric("Local addresses", snapshot.localAddresses.joinToString("\n").ifEmpty { "Unavailable" })
    }
}

@Composable
private fun DnsScreen(viewModel: DnsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    ScreenList {
        item {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setInput,
                label = { Text(if (state.type == DnsRecordType.PTR) "IP address" else "Domain") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { expanded = true }) { Text(state.type.name) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DnsRecordType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            viewModel.setType(type)
                            expanded = false
                        },
                    )
                }
            }
            Button(onClick = viewModel::query, enabled = state.result !is ToolState.Loading) {
                Text("Query DNS")
            }
            ToolResult(state.result) { DnsResultView(it) }
        }
    }
}

@Composable
private fun DnsResultView(result: DnsResult) {
    Metric("Status", result.status.toString())
    Metric("Timing", "${result.elapsedMs} ms")
    Metric("Authenticated data", result.authenticatedData.yesNo())
    result.records.forEach {
        Metric("${it.name} · TTL ${it.ttlSeconds}", it.value)
    }
    if (result.records.isEmpty()) InfoCard("The response contained no answer records.")
    result.authority.forEach {
        Metric("Authority · ${it.name} · TTL ${it.ttlSeconds}", it.value)
    }
    result.additional.forEach {
        Metric("Additional · ${it.name} · TTL ${it.ttlSeconds}", it.value)
    }
}

@Composable
private fun TlsScreen(viewModel: TlsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item {
            EndpointField(state.input, viewModel::setInput)
            Button(onClick = viewModel::inspect, enabled = state.result !is ToolState.Loading) {
                Text("Inspect certificate")
            }
            ToolResult(state.result) { TlsResultView(it) }
        }
    }
}

@Composable
private fun TlsResultView(result: TlsResult) {
    Metric("Connection", "${result.host}:${result.port}")
    Metric("Protocol", result.protocol)
    Metric("Cipher suite", result.cipherSuite)
    Metric("Timing", "${result.elapsedMs} ms")
    result.certificates.forEachIndexed { index, certificate ->
        Text("Certificate ${index + 1}", style = MaterialTheme.typography.titleMedium)
        Metric("Subject", certificate.subject)
        Metric("Issuer", certificate.issuer)
        Metric("Valid from", certificate.validFrom.toString())
        Metric("Valid until", certificate.validUntil.toString())
        Metric("Serial number", certificate.serialNumber)
        Metric("Algorithms", "${certificate.publicKeyAlgorithm} / ${certificate.signatureAlgorithm}")
        Metric("Subject alternative names", certificate.subjectAlternativeNames.joinToString())
    }
}

@Composable
private fun HttpScreen(viewModel: HttpViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item {
            EndpointField(state.input, viewModel::setInput)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HttpMethod.entries.forEach { method ->
                    OutlinedButton(onClick = { viewModel.setMethod(method) }) {
                        Text(if (state.method == method) "Selected ${method.name}" else method.name)
                    }
                }
            }
            Button(onClick = viewModel::inspect, enabled = state.result !is ToolState.Loading) {
                Text("Inspect response")
            }
            ToolResult(state.result) { HttpResultView(it) }
        }
    }
}

@Composable
private fun HttpResultView(result: HttpInspection) {
    Metric("Status", "${result.status} ${result.message}")
    Metric("Final URL", result.finalUrl)
    Metric("Protocol", result.protocol)
    Metric("Timing", "${result.elapsedMs} ms")
    result.redirects.forEach { Metric("Redirect ${it.status}", "${it.from}\n${it.to}") }
    Text("Headers", style = MaterialTheme.typography.titleMedium)
    result.headers.forEach { Metric(it.name, it.value) }
    result.bodyPreview?.let {
        Text("Body preview${if (result.bodyTruncated) " (truncated)" else ""}")
        InfoCard(it)
    }
}

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    ScreenList {
        item {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            ThemeMode.entries.forEach { mode ->
                ChoiceRow(mode.name.lowercase().replaceFirstChar(Char::uppercase), settings.themeMode == mode) {
                    viewModel.setThemeMode(mode)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Optional analytics", style = MaterialTheme.typography.titleLarge)
            Text("No analytics SDK is installed. This preference is stored for a future opt-in integration.")
            AnalyticsConsent.entries.forEach { consent ->
                ChoiceRow(
                    consent.name.lowercase().replaceFirstChar(Char::uppercase),
                    settings.analyticsConsent == consent,
                ) { viewModel.setConsent(consent) }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.padding(top = 12.dp))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SpeedScreen(
    settingsViewModel: SettingsViewModel,
    viewModel: SpeedViewModel = koinViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item {
            InfoCard(
                "This test sends and receives up to 7.2 MB through Cloudflare. " +
                    "Cloudflare receives your IP address as part of the connection.",
            )
            if (!settings.cloudflareDisclosureAccepted) {
                Button(onClick = settingsViewModel::acceptCloudflareDisclosure) {
                    Text("I understand")
                }
            } else {
                Button(onClick = viewModel::runTest, enabled = state !is ToolState.Loading) {
                    Text("Run speed test")
                }
                ToolResult(state) { SpeedResultView(it) }
            }
        }
    }
}

@Composable
private fun SpeedResultView(result: SpeedResult) {
    Metric("Latency", "%.1f ms".format(result.latencyMs))
    Metric("Jitter", "%.1f ms".format(result.jitterMs))
    Metric("Download", "%.1f Mbps".format(result.downloadMbps))
    Metric("Upload", "%.1f Mbps".format(result.uploadMbps))
    Metric("Transferred", "%.2f MB".format(result.transferredBytes / 1_000_000.0))
    Metric("Measured", result.measuredAt.toString())
}

@Composable
private fun AboutScreen() {
    ScreenList {
        item {
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Text(
                "Debug Toolkit stores only theme and consent preferences. Diagnostic " +
                    "results are not persisted. DNS and speed tests contact Cloudflare; " +
                    "certificate and HTTP tools contact the endpoint you enter.",
            )
            Spacer(Modifier.height(16.dp))
            Text("Measurements", style = MaterialTheme.typography.titleLarge)
            Text(
                "Wi-Fi dBm indicates signal strength, not physical distance. Speed results " +
                    "are point-in-time estimates affected by device and network conditions.",
            )
        }
    }
}

@Composable
private fun EndpointField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("HTTPS endpoint") },
        placeholder = { Text("example.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> ToolResult(state: ToolState<T>, content: @Composable (T) -> Unit) {
    when (state) {
        ToolState.Idle -> Unit
        ToolState.Loading -> CircularProgressIndicator()
        is ToolState.Error -> InfoCard(state.message)
        is ToolState.Success -> content(state.value)
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value)
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ScreenList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

private fun Boolean.yesNo() = if (this) "Yes" else "No"
