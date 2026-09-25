package co.sorsby.debugtoolkit.data.lan

import co.sorsby.debugtoolkit.core.model.LanDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import kotlin.time.TimeSource

/** Probes a single host to see whether it answers on the local network. */
interface HostProber {
    suspend fun probe(host: String): LanDevice?
}

/**
 * Probes a host with [InetAddress.isReachable], which tries an ICMP echo first and transparently
 * falls back to a TCP connection on port 7 when ICMP is not permitted, which is always the case
 * for an unprivileged app on Android. This is why a local network sweep does not require root.
 */
class ReachabilityHostProber(private val timeoutMs: Int = 800) : HostProber {
    override suspend fun probe(host: String): LanDevice? = withContext(Dispatchers.IO) {
        val address = InetAddress.getByName(host)
        val mark = TimeSource.Monotonic.markNow()
        if (!address.isReachable(timeoutMs)) return@withContext null
        LanDevice(
            ipAddress = host,
            hostname = address.canonicalHostName.takeIf { it != host },
            responseTimeMs = mark.elapsedNow().inWholeMilliseconds,
        )
    }
}
