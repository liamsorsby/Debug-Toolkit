package co.sorsby.debugtoolkit.data.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.NetworkTransport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface NetworkMonitor {
    val snapshots: Flow<NetworkSnapshot>
}

class AndroidNetworkMonitor(
    private val context: Context,
    private val connectivityManager: ConnectivityManager,
    private val wifiManager: WifiManager,
) : NetworkMonitor {
    override val snapshots: Flow<NetworkSnapshot> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(snapshot())
            }

            override fun onLost(network: Network) {
                trySend(snapshot())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(snapshot())
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: android.net.LinkProperties,
            ) {
                trySend(snapshot())
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        trySend(snapshot())

        // Wi-Fi signal strength does not reliably trigger onCapabilitiesChanged on every
        // OEM skin, so this broadcast receiver provides genuinely event-driven RSSI updates:
        // it fires only when the system detects an actual signal-strength change, rather than
        // on a fixed timer.
        val rssiReceiver = object : BroadcastReceiver() {
            override fun onReceive(receivedContext: Context, intent: Intent) {
                trySend(snapshot())
            }
        }
        val rssiFilter = IntentFilter(WifiManager.RSSI_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(rssiReceiver, rssiFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(rssiReceiver, rssiFilter)
        }

        awaitClose {
            context.unregisterReceiver(rssiReceiver)
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    private fun snapshot(): NetworkSnapshot {
        val sampledAt = System.currentTimeMillis()
        val network = connectivityManager.activeNetwork
            ?: return NetworkSnapshotFactory.disconnected(sampledAt)
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkSnapshotFactory.disconnected(sampledAt)
        return NetworkSnapshotFactory.connected(
            sampledAtEpochMillis = sampledAt,
            transports = capabilities.transports(),
            validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            notMetered = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
            ),
            localAddresses = connectivityManager.getLinkProperties(network)
                ?.linkAddresses
                .orEmpty()
                .map { it.address.hostAddress.orEmpty() },
            wifiRssiDbm = wifiRssi(capabilities),
            linkDownstreamKbps = capabilities.linkDownstreamBandwidthKbps,
            linkUpstreamKbps = capabilities.linkUpstreamBandwidthKbps,
        )
    }

    private fun NetworkCapabilities.transports(): Set<NetworkTransport> = buildSet {
        TRANSPORTS.forEach { (platformTransport, transport) ->
            if (hasTransport(platformTransport)) add(transport)
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun wifiRssi(capabilities: NetworkCapabilities): Int? {
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            !hasWifiPermission()
        ) {
            return null
        }
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            capabilities.transportInfo as? WifiInfo
        } else {
            wifiManager.connectionInfo
        }
        return info?.rssi?.let(NetworkSnapshotFactory::usableRssi)
    }

    private fun hasWifiPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        val TRANSPORTS = mapOf(
            NetworkCapabilities.TRANSPORT_WIFI to NetworkTransport.WIFI,
            NetworkCapabilities.TRANSPORT_CELLULAR to NetworkTransport.CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET to NetworkTransport.ETHERNET,
            NetworkCapabilities.TRANSPORT_VPN to NetworkTransport.VPN,
            NetworkCapabilities.TRANSPORT_BLUETOOTH to NetworkTransport.BLUETOOTH,
        )
    }
}
