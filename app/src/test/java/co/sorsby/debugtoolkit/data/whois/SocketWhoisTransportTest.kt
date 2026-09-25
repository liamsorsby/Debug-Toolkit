package co.sorsby.debugtoolkit.data.whois

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ServerSocket

/**
 * Exercises [SocketWhoisTransport] against a real loopback TCP server rather than a mock,
 * matching [co.sorsby.debugtoolkit.data.dns.RawDnsResolverTest]'s approach, so the wire
 * behaviour (query line terminated with CRLF, response read until the server closes) is proven
 * end-to-end.
 */
class SocketWhoisTransportTest {

    @Test
    fun `sends the query and reads the full response until the server closes`() = runTest {
        ServerSocket(0).use { server ->
            val transport = SocketWhoisTransport()
            var receivedQuery = ""
            val serverThread = Thread {
                server.accept().use { connection ->
                    receivedQuery = connection.getInputStream().bufferedReader().readLine()
                    connection.getOutputStream().write("domain: EXAMPLE.COM\r\nstatus: ACTIVE\r\n".toByteArray())
                }
            }
            serverThread.start()

            val response = transport.query("127.0.0.1", server.localPort, "example.com")

            serverThread.join(2_000)
            assertEquals("example.com", receivedQuery)
            assertEquals("domain: EXAMPLE.COM\r\nstatus: ACTIVE\r\n", response)
        }
    }
}
