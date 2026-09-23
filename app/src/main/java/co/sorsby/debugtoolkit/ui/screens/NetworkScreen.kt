package co.sorsby.debugtoolkit.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.NetworkTransport
import co.sorsby.debugtoolkit.core.model.WifiSignal
import co.sorsby.debugtoolkit.feature.NetworkViewModel
import co.sorsby.debugtoolkit.ui.components.InfoCard
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.SectionHeader
import co.sorsby.debugtoolkit.ui.components.yesNo
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun NetworkRoute(viewModel: NetworkViewModel = koinViewModel()) {
    val context = LocalContext.current
    val permissions = requiredWifiPermissions()
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

    NetworkScreen(
        snapshot = snapshot,
        permissionGranted = permissionGranted,
        onRequestPermission = { launcher.launch(permissions) },
    )
}

@Composable
fun NetworkScreen(
    snapshot: NetworkSnapshot,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    ScreenList {
        item { LiveNetworkHero(snapshot) }
        if (!permissionGranted) {
            item { PermissionCard(onRequestPermission) }
        }
        item {
            SectionHeader(
                stringResource(R.string.network_details_title),
                stringResource(R.string.network_details_supporting),
            )
        }
        item { NetworkDetails(snapshot) }
        item { InfoCard(stringResource(R.string.network_capacity_explanation)) }
    }
}

private fun requiredWifiPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
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
    val transportLabels = snapshot.transports.map { stringResource(it.labelResource()) }
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
private fun LiveIndicator(color: Color) {
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
        stringResource(R.string.network_signal_accessibility, it, signalLabel(it))
    } ?: stringResource(R.string.network_signal_accessibility_unavailable)

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(104.dp)
            .semantics { contentDescription = signalDescription },
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
                topLeft = Offset(left, size.height - height),
                size = Size(barWidth, height),
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
private fun NetworkTransport.labelResource(): Int = when (this) {
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
            DetailRow(stringResource(R.string.network_metered), yesNo(snapshot.metered))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                stringResource(R.string.network_downstream_capacity),
                snapshot.estimatedDownstreamKbps?.let { rateLabel(it) }
                    ?: stringResource(R.string.value_unavailable),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                stringResource(R.string.network_upstream_capacity),
                snapshot.estimatedUpstreamKbps?.let { rateLabel(it) }
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

@Preview(showBackground = true)
@Composable
private fun NetworkScreenPreview() {
    DebugToolkitTheme {
        NetworkScreen(
            snapshot = NetworkSnapshot(
                connected = true,
                validated = true,
                transports = setOf(NetworkTransport.WIFI),
                localAddresses = listOf("192.0.2.10"),
                wifiRssiDbm = -52,
                wifiSignal = WifiSignal.GOOD,
                estimatedDownstreamKbps = 30_000,
                estimatedUpstreamKbps = 12_000,
            ),
            permissionGranted = true,
            onRequestPermission = {},
        )
    }
}
