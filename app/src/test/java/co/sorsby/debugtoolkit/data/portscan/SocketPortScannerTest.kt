package co.sorsby.debugtoolkit.data.portscan

import co.sorsby.debugtoolkit.core.model.PortState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.ServerSocket

/**
 * Exercises [SocketPortScanner] against real loopback sockets rather than mocks, matching
 * [co.sorsby.debugtoolkit.data.dns.RawDnsResolverTest]'s approach. Only the open and closed
 * classifications are deterministic on a local machine; the filtered (timeout) classification
 * depends on real network conditions and is covered by manual testing against a firewalled host.
 */
class SocketPortScannerTest {
    @Test
    fun `classifies a listening port as open`() = runTest {
        ServerSocket(0).use { server ->
            val scanner = SocketPortScanner()

            val result = scanner.scan("127.0.0.1", listOf(server.localPort))

            assertEquals(PortState.OPEN, result.entries.single().state)
        }
    }

    @Test
    fun `classifies a closed port as closed`() = runTest {
        val port = ServerSocket(0).use { it.localPort }
        val scanner = SocketPortScanner()

        val result = scanner.scan("127.0.0.1", listOf(port))

        assertEquals(PortState.CLOSED, result.entries.single().state)
    }

    @Test
    fun `scans every requested port and sorts the results`() = runTest {
        ServerSocket(0).use { serverA ->
            ServerSocket(0).use { serverB ->
                val scanner = SocketPortScanner()
                val ports = listOf(serverB.localPort, serverA.localPort)

                val result = scanner.scan("127.0.0.1", ports)

                assertEquals(ports.sorted(), result.entries.map { it.port })
                assertEquals(true, result.entries.all { it.state == PortState.OPEN })
            }
        }
    }

    @Test
    fun `rejects an empty port list`() = runTest {
        val scanner = SocketPortScanner()

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { scanner.scan("127.0.0.1", emptyList()) }
        }
    }

    @Test
    fun `rejects more ports than the cap allows`() = runTest {
        val scanner = SocketPortScanner()
        val tooManyPorts = (1..PortListParser.MAX_PORTS + 1).toList()

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { scanner.scan("127.0.0.1", tooManyPorts) }
        }
    }
}
