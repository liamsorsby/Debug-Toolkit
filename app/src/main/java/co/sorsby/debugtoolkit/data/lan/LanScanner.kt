package co.sorsby.debugtoolkit.data.lan

import co.sorsby.debugtoolkit.core.model.LanScanResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.TimeSource

interface LanScanner {
    suspend fun scan(): LanScanResult
}

/**
 * Sweeps the device's local subnet for other reachable hosts. [currentAddress] supplies the
 * device's own private IPv4 address and network prefix length; it is injected as a function
 * rather than this class reading `ConnectivityManager` directly, so the sweep, capping, and
 * concurrency logic here can be unit tested without an Android framework or device, matching how
 * [co.sorsby.debugtoolkit.data.network.AndroidNetworkMonitor] keeps its own framework calls thin.
 */
class SweepLanScanner(
    private val currentAddress: () -> Pair<String, Int>?,
    private val prober: HostProber = ReachabilityHostProber(),
) : LanScanner {
    override suspend fun scan(): LanScanResult = coroutineScope {
        val (address, prefixLength) = currentAddress()
            ?: throw IllegalStateException(
                "Connect to a Wi-Fi or Ethernet network with a private IPv4 address to scan.",
            )
        check(prefixLength in SubnetRange.MIN_PREFIX_LENGTH..SubnetRange.MAX_PREFIX_LENGTH) {
            "This network is too large to sweep from this device."
        }
        val hosts = SubnetRange.hostAddresses(address, prefixLength)

        val mark = TimeSource.Monotonic.markNow()
        val semaphore = Semaphore(MAX_CONCURRENT_PROBES)
        val devices = hosts.map { host ->
            async { semaphore.withPermit { prober.probe(host) } }
        }.awaitAll().filterNotNull().sortedBy { SubnetRange.sortKey(it.ipAddress) }

        LanScanResult(
            subnetCidr = SubnetRange.cidr(address, prefixLength),
            devices = devices,
            addressesScanned = hosts.size,
            elapsedMs = mark.elapsedNow().inWholeMilliseconds,
        )
    }

    private companion object {
        const val MAX_CONCURRENT_PROBES = 32
    }
}
