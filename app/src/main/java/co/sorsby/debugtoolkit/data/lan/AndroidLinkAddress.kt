package co.sorsby.debugtoolkit.data.lan

import android.net.ConnectivityManager
import java.net.Inet4Address

/**
 * Reads the device's current private IPv4 address and prefix length from the active network's
 * link properties. This is the one Android-framework-dependent piece of the LAN scanner; it is
 * kept to a single call so the rest of the sweep logic in [SweepLanScanner] can be unit tested
 * without it, matching [co.sorsby.debugtoolkit.data.network.AndroidNetworkMonitor]'s approach.
 */
fun ConnectivityManager.currentPrivateIpv4Address(): Pair<String, Int>? {
    val network = activeNetwork ?: return null
    val addresses = getLinkProperties(network)
        ?.linkAddresses
        .orEmpty()
        .filter { it.address is Inet4Address }
        .map { it.address.hostAddress.orEmpty() to it.prefixLength }
        .filter { (address, _) -> address.isNotEmpty() }
    return LinkAddressSelector.selectPrivateIpv4(addresses)
}
