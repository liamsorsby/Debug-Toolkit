package co.sorsby.debugtoolkit.data.portscan

import co.sorsby.debugtoolkit.core.model.PortScanEntry
import co.sorsby.debugtoolkit.core.model.PortScanResult
import co.sorsby.debugtoolkit.core.model.PortState
import co.sorsby.debugtoolkit.domain.InputValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.time.TimeSource

interface PortScanner {
    suspend fun scan(host: String, ports: List<Int>): PortScanResult
}

/**
 * Probes each port with a plain TCP connect attempt, classifying it open (connected), closed
 * (connection actively refused), or filtered (no response before the timeout, the classic sign
 * of a firewall silently dropping the packet). Concurrency and the port count are both capped
 * so this cannot be turned into a high-volume scanning tool from a single tap; see
 * [PortListParser.MAX_PORTS] and [MAX_CONCURRENT_PROBES].
 */
class SocketPortScanner(
    private val connectTimeoutMs: Int = 500,
) : PortScanner {
    override suspend fun scan(host: String, ports: List<Int>): PortScanResult = coroutineScope {
        val target = InputValidation.host(host)
        require(ports.isNotEmpty()) { "Enter at least one port." }
        require(ports.size <= PortListParser.MAX_PORTS) {
            "Scan at most ${PortListParser.MAX_PORTS} ports at a time."
        }

        val mark = TimeSource.Monotonic.markNow()
        val semaphore = Semaphore(MAX_CONCURRENT_PROBES)
        val entries = ports.map { port ->
            async(Dispatchers.IO) {
                semaphore.withPermit { probe(target, port) }
            }
        }.awaitAll()

        PortScanResult(
            host = target,
            entries = entries.sortedBy { it.port },
            elapsedMs = mark.elapsedNow().inWholeMilliseconds,
        )
    }

    private suspend fun probe(host: String, port: Int): PortScanEntry = withContext(Dispatchers.IO) {
        val state = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                PortState.OPEN
            }
        } catch (timeout: SocketTimeoutException) {
            PortState.FILTERED
        } catch (io: IOException) {
            PortState.CLOSED
        }
        PortScanEntry(port, state)
    }

    private companion object {
        const val MAX_CONCURRENT_PROBES = 16
    }
}
