package co.sorsby.debugtoolkit.ui

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.ListFormatter
import android.os.Build
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
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
import co.sorsby.debugtoolkit.core.model.NetworkTransport
import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.core.model.ToolState
import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.core.model.WifiSignal
import co.sorsby.debugtoolkit.R
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private data class Destination(
    val route: String,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    Destination("overview", R.string.nav_overview, R.string.nav_overview_description, Icons.Default.Home),
    Destination("network", R.string.nav_network, R.string.nav_network_description, Icons.Default.Wifi),
    Destination("tools", R.string.nav_tools, R.string.nav_tools_description, Icons.Default.Build),
)

private val drawerDestinations = listOf(
    Destination("speed", R.string.nav_speed, R.string.nav_speed_description, Icons.Default.Speed),
    Destination("tls", R.string.nav_tls, R.string.nav_tls_description, Icons.Default.Security),
    Destination("dns", R.string.nav_dns, R.string.nav_dns_description, Icons.Default.Dns),
    Destination("http", R.string.nav_http, R.string.nav_http_description, Icons.Default.Language),
    Destination("settings", R.string.nav_settings, R.string.nav_settings_description, Icons.Default.Settings),
    Destination("about", R.string.nav_about, R.string.nav_about_description, Icons.Default.Info),
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
        .firstOrNull { it.route == route }?.label ?: R.string.app_name

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.app_tagline),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(12.dp))
                drawerDestinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(destination.label)) },
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
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(title)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.navigation_open),
                            )
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
                            label = { Text(stringResource(destination.label)) },
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
            HeroCard()
        }
        item {
            SectionHeader(
                stringResource(R.string.overview_tools_title),
                stringResource(R.string.overview_tools_supporting),
            )
        }
        items(drawerDestinations.take(4)) { destination ->
            ToolCard(destination) { navController.navigateSingle(destination.route) }
        }
    }
}

@Composable
private fun ToolsScreen(navController: NavHostController) {
    ScreenList {
        item {
            SectionHeader(
                stringResource(R.string.tools_title),
                stringResource(R.string.tools_supporting),
            )
        }
        items(drawerDestinations.take(4)) { destination ->
            ToolCard(destination) { navController.navigateSingle(destination.route) }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Text(
                stringResource(R.string.overview_hero_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.overview_hero_body),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, supportingText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ToolCard(destination: Destination, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    destination.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(stringResource(destination.label), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(destination.description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            LiveNetworkHero(snapshot)
        }
        if (!permissionGranted) {
            item {
                PermissionCard { launcher.launch(permissions) }
            }
        }
        item {
            SectionHeader(
                stringResource(R.string.network_details_title),
                stringResource(R.string.network_details_supporting),
            )
        }
        item {
            NetworkDetails(snapshot)
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.network_permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.network_permission_body),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Button(onClick = onRequest) {
                Text(stringResource(R.string.network_permission_action))
            }
        }
    }
}

@Composable
private fun LiveNetworkHero(snapshot: NetworkSnapshot) {
    val transportLabels = mutableListOf<String>()
    for (transport in snapshot.transports) {
        transportLabels += stringResource(transport.labelResource())
    }
    val transportSummary = if (transportLabels.isEmpty()) {
        stringResource(R.string.network_no_network)
    } else {
        ListFormatter.getInstance().format(transportLabels)
    }
    val connectedColor by animateColorAsState(
        targetValue = if (snapshot.validated) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        label = "connection color",
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveIndicator(connectedColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.network_monitoring_live),
                    style = MaterialTheme.typography.labelSmall,
                    color = connectedColor,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        transportSummary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            SignalMeter(
                rssiDbm = snapshot.wifiRssiDbm,
                signal = snapshot.wifiSignal,
                connected = snapshot.connected,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                when {
                    !snapshot.connected -> stringResource(R.string.network_no_connection)
                    snapshot.validated -> stringResource(R.string.network_ready)
                    else -> stringResource(R.string.network_not_validated)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                snapshot.wifiRssiDbm?.let {
                    stringResource(R.string.network_signal_value, it, signalLabel(it))
                } ?: stringResource(R.string.network_signal_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveIndicator(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "live pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live pulse alpha",
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(color.copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun SignalMeter(
    rssiDbm: Int?,
    signal: WifiSignal,
    connected: Boolean,
) {
    val targetStrength = when {
        !connected -> 0f
        rssiDbm != null -> ((rssiDbm + 100) / 50f).coerceIn(0f, 1f)
        else -> 0f
    }
    val strength by animateFloatAsState(
        targetValue = targetStrength,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "signal strength",
    )
    val activeColor = when (signal) {
        WifiSignal.EXCELLENT -> MaterialTheme.colorScheme.primary
        WifiSignal.GOOD -> MaterialTheme.colorScheme.tertiary
        WifiSignal.FAIR -> MaterialTheme.colorScheme.secondary
        WifiSignal.WEAK -> MaterialTheme.colorScheme.error
        WifiSignal.UNAVAILABLE -> MaterialTheme.colorScheme.outline
    }
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val signalDescription = rssiDbm?.let {
        stringResource(
            R.string.network_signal_accessibility,
            it,
            signalLabel(it),
        )
    } ?: stringResource(R.string.network_signal_accessibility_unavailable)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(104.dp)
            .semantics {
                contentDescription = signalDescription
            },
    ) {
        val barCount = 5
        val gap = 10.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        repeat(barCount) { index ->
            val progress = (index + 1) / barCount.toFloat()
            val height = size.height * (0.25f + index * 0.1875f)
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = if (strength + 0.001f >= progress) activeColor else inactiveColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
        }
    }
}

@Composable
private fun signalLabel(rssiDbm: Int): String = when {
    rssiDbm >= -50 -> stringResource(R.string.signal_excellent)
    rssiDbm >= -60 -> stringResource(R.string.signal_good)
    rssiDbm >= -70 -> stringResource(R.string.signal_fair)
    else -> stringResource(R.string.signal_weak)
}

@StringRes
private fun NetworkTransport.labelResource(): Int =
    when (this) {
        NetworkTransport.WIFI -> R.string.network_transport_wifi
        NetworkTransport.CELLULAR -> R.string.network_transport_cellular
        NetworkTransport.ETHERNET -> R.string.network_transport_ethernet
        NetworkTransport.VPN -> R.string.network_transport_vpn
        NetworkTransport.BLUETOOTH -> R.string.network_transport_bluetooth
    }

@Composable
private fun NetworkDetails(snapshot: NetworkSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            DetailRow(
                stringResource(R.string.network_internet),
                stringResource(
                    if (snapshot.validated) R.string.value_validated
                    else R.string.value_not_validated,
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                stringResource(R.string.network_metered),
                yesNo(snapshot.metered),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                stringResource(R.string.network_downstream),
                snapshot.linkDownKbps?.let { rateLabel(it) }
                    ?: stringResource(R.string.value_unavailable),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                stringResource(R.string.network_upstream),
                snapshot.linkUpKbps?.let { rateLabel(it) }
                    ?: stringResource(R.string.value_unavailable),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                stringResource(R.string.network_local_addresses),
                snapshot.localAddresses.joinToString("\n")
                    .ifEmpty { stringResource(R.string.value_unavailable) },
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.44f),
        )
        Text(
            value,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.56f),
        )
    }
}

@Composable
private fun rateLabel(kbps: Int): String =
    if (kbps >= 1_000) {
        stringResource(R.string.rate_mbps, kbps / 1_000f)
    } else {
        stringResource(R.string.rate_kbps, kbps)
    }

@Composable
private fun DnsScreen(viewModel: DnsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Dns,
                description = stringResource(R.string.dns_intro),
            )
        }
        item {
            ToolInputCard {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::setInput,
                    label = {
                        Text(
                            stringResource(
                                if (state.type == DnsRecordType.PTR) R.string.dns_ip_label
                                else R.string.dns_domain_label,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (state.type == DnsRecordType.PTR) {
                            KeyboardType.Ascii
                        } else {
                            KeyboardType.Uri
                        },
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.dns_record_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(state.type.name)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
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
                }
                Button(
                    onClick = viewModel::query,
                    enabled = state.result !is ToolState.Loading && state.input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dns_query_action))
                }
            }
        }
        item {
            ToolResult(state.result) { DnsResultView(it) }
        }
    }
}

@Composable
private fun DnsResultView(result: DnsResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.dns_status), result.status.toString())
        Metric(
            stringResource(R.string.dns_timing),
            stringResource(R.string.duration_milliseconds_integer, result.elapsedMs),
        )
        Metric(stringResource(R.string.dns_authenticated), yesNo(result.authenticatedData))
        result.records.forEach {
            Metric(stringResource(R.string.dns_answer_label, it.name, it.ttlSeconds), it.value)
        }
        if (result.records.isEmpty()) {
            InfoCard(stringResource(R.string.dns_no_answers))
        }
        result.authority.forEach {
            Metric(stringResource(R.string.dns_authority_label, it.name, it.ttlSeconds), it.value)
        }
        result.additional.forEach {
            Metric(stringResource(R.string.dns_additional_label, it.name, it.ttlSeconds), it.value)
        }
    }
}

@Composable
private fun TlsScreen(viewModel: TlsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Security,
                description = stringResource(R.string.tls_intro),
            )
        }
        item {
            ToolInputCard {
                EndpointField(state.input, viewModel::setInput)
                Button(
                    onClick = viewModel::inspect,
                    enabled = state.result !is ToolState.Loading && state.input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.tls_action))
                }
            }
        }
        item {
            ToolResult(state.result) { TlsResultView(it) }
        }
    }
}

@Composable
private fun TlsResultView(result: TlsResult) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.tls_connection), "${result.host}:${result.port}")
        Metric(stringResource(R.string.tls_protocol), result.protocol)
        Metric(stringResource(R.string.tls_cipher_suite), result.cipherSuite)
        Metric(
            stringResource(R.string.tls_timing),
            stringResource(R.string.duration_milliseconds_integer, result.elapsedMs),
        )
        result.certificates.forEachIndexed { index, certificate ->
            SectionLabel(stringResource(R.string.tls_certificate_number, index + 1))
            Metric(stringResource(R.string.tls_subject), certificate.subject)
            Metric(stringResource(R.string.tls_issuer), certificate.issuer)
            Metric(
                stringResource(R.string.tls_valid_from),
                formatLocalizedInstant(certificate.validFrom),
            )
            Metric(
                stringResource(R.string.tls_valid_until),
                formatLocalizedInstant(certificate.validUntil),
            )
            Metric(stringResource(R.string.tls_serial_number), certificate.serialNumber)
            Metric(
                stringResource(R.string.tls_algorithms),
                "${certificate.publicKeyAlgorithm} / ${certificate.signatureAlgorithm}",
            )
            Metric(
                stringResource(R.string.tls_subject_alternative_names),
                ListFormatter.getInstance().format(certificate.subjectAlternativeNames),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HttpScreen(viewModel: HttpViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Language,
                description = stringResource(R.string.http_intro),
            )
        }
        item {
            ToolInputCard {
                EndpointField(state.input, viewModel::setInput)
                Text(
                    stringResource(R.string.http_method),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HttpMethod.entries.forEachIndexed { index, method ->
                        SegmentedButton(
                            selected = state.method == method,
                            onClick = { viewModel.setMethod(method) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HttpMethod.entries.size,
                            ),
                            label = { Text(method.name) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Button(
                    onClick = viewModel::inspect,
                    enabled = state.result !is ToolState.Loading && state.input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.http_action))
                }
            }
        }
        item {
            ToolResult(state.result) { HttpResultView(it) }
        }
    }
}

@Composable
private fun HttpResultView(result: HttpInspection) {
    ResultCard {
        ResultHeader()
        Metric(stringResource(R.string.http_status), "${result.status} ${result.message}")
        Metric(stringResource(R.string.http_final_url), result.finalUrl)
        Metric(stringResource(R.string.http_protocol), result.protocol)
        Metric(
            stringResource(R.string.http_timing),
            stringResource(R.string.duration_milliseconds_integer, result.elapsedMs),
        )
        result.redirects.forEach {
            Metric(stringResource(R.string.http_redirect, it.status), "${it.from}\n${it.to}")
        }
        SectionLabel(stringResource(R.string.http_headers))
        result.headers.forEach { Metric(it.name, it.value) }
        result.bodyPreview?.let {
            SectionLabel(
                stringResource(
                    if (result.bodyTruncated) R.string.http_body_preview_truncated
                    else R.string.http_body_preview,
                ),
            )
            InfoCard(it)
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    ScreenList {
        item {
            SettingsCard(
                title = stringResource(R.string.settings_appearance),
                supportingText = stringResource(R.string.settings_appearance_supporting),
            ) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceRow(
                        label = themeModeLabel(mode),
                        selected = settings.themeMode == mode,
                    ) {
                        viewModel.setThemeMode(mode)
                    }
                }
            }
        }
        item {
            SettingsCard(
                title = stringResource(R.string.settings_analytics),
                supportingText = stringResource(R.string.settings_analytics_supporting),
            ) {
                AnalyticsConsent.entries.forEach { consent ->
                    ChoiceRow(
                        label = consentLabel(consent),
                        selected = settings.analyticsConsent == consent,
                    ) { viewModel.setConsent(consent) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun SettingsCard(
    title: String,
    supportingText: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun consentLabel(consent: AnalyticsConsent): String = stringResource(
    when (consent) {
        AnalyticsConsent.UNSET -> R.string.consent_unset
        AnalyticsConsent.DENIED -> R.string.consent_denied
        AnalyticsConsent.GRANTED -> R.string.consent_granted
    },
)

@Composable
private fun SpeedScreen(
    settingsViewModel: SettingsViewModel,
    viewModel: SpeedViewModel = koinViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Speed,
                description = stringResource(R.string.speed_intro),
            )
        }
        item {
            ToolInputCard {
                InfoCard(stringResource(R.string.speed_disclosure))
                if (!settings.cloudflareDisclosureAccepted) {
                    FilledTonalButton(
                        onClick = settingsViewModel::acceptCloudflareDisclosure,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.speed_disclosure_action))
                    }
                } else {
                    Button(
                        onClick = viewModel::runTest,
                        enabled = state !is ToolState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.speed_action))
                    }
                }
            }
        }
        item {
            ToolResult(state) { SpeedResultView(it) }
        }
    }
}

@Composable
private fun SpeedResultView(result: SpeedResult) {
    ResultCard {
        ResultHeader()
        Metric(
            stringResource(R.string.speed_latency),
            stringResource(R.string.duration_milliseconds, result.latencyMs),
        )
        Metric(
            stringResource(R.string.speed_jitter),
            stringResource(R.string.duration_milliseconds, result.jitterMs),
        )
        Metric(
            stringResource(R.string.speed_download),
            stringResource(R.string.rate_mbps, result.downloadMbps),
        )
        Metric(
            stringResource(R.string.speed_upload),
            stringResource(R.string.rate_mbps, result.uploadMbps),
        )
        Metric(
            stringResource(R.string.speed_transferred),
            stringResource(R.string.size_megabytes, result.transferredBytes / 1_000_000.0),
        )
        Metric(stringResource(R.string.speed_measured), formatLocalizedInstant(result.measuredAt))
    }
}

private fun formatLocalizedInstant(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(instant)

@Composable
private fun AboutScreen() {
    ScreenList {
        item {
            InformationCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.about_privacy),
                body = stringResource(R.string.about_privacy_body),
            )
        }
        item {
            InformationCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_measurements),
                body = stringResource(R.string.about_measurements_body),
            )
        }
    }
}

@Composable
private fun EndpointField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.endpoint_label)) },
        placeholder = { Text(stringResource(R.string.endpoint_example)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> ToolResult(state: ToolState<T>, content: @Composable (T) -> Unit) {
    when (state) {
        ToolState.Idle -> Unit
        ToolState.Loading -> LoadingCard()
        is ToolState.Error -> ErrorCard(state.type)
        is ToolState.Success -> content(state.value)
    }
}

@Composable
private fun ToolIntroCard(icon: ImageVector, description: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Text(
                description,
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ToolInputCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.tool_input_title), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ResultCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun ResultHeader() {
    Text(
        stringResource(R.string.tool_results_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun LoadingCard() {
    val description = stringResource(R.string.tool_running)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text(
                stringResource(R.string.tool_running),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ErrorCard(error: ToolError) {
    Card(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.tool_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(
                    when (error) {
                        ToolError.INVALID_INPUT -> R.string.tool_error_invalid_input
                        ToolError.NETWORK -> R.string.tool_error_network
                        ToolError.SERVICE -> R.string.tool_error_service
                        ToolError.UNKNOWN -> R.string.tool_error_unknown
                    },
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun InformationCard(icon: ImageVector, title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(value, color = MaterialTheme.colorScheme.onSurface)
        }
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun yesNo(value: Boolean) = stringResource(
    if (value) R.string.value_yes else R.string.value_no,
)
