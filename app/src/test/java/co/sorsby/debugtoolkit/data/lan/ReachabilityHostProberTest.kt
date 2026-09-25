package co.sorsby.debugtoolkit.data.lan

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.UnknownHostException

/**
 * Exercises [ReachabilityHostProber] against real addresses rather than mocks. Loopback is
 * always reachable, which gives a deterministic "device found" case; an address reserved for
 * documentation (RFC 5737) that nothing binds to gives a deterministic "device not found" case
 * without depending on real network conditions.
 */
class ReachabilityHostProberTest {

    @Test
    fun `finds a reachable host`() = runTest {
        val prober = ReachabilityHostProber(timeoutMs = 1_000)

        val device = prober.probe("127.0.0.1")

        assertEquals("127.0.0.1", device?.ipAddress)
    }

    @Test
    fun `throws for a host name that cannot be resolved`() = runTest {
        val prober = ReachabilityHostProber(timeoutMs = 200)

        org.junit.Assert.assertThrows(UnknownHostException::class.java) {
            kotlinx.coroutines.runBlocking { prober.probe("not.a.valid.host.invalid") }
        }
    }

    @Test
    fun `returns null for an unreachable documentation address`() = runTest {
        val prober = ReachabilityHostProber(timeoutMs = 200)

        val device = prober.probe("192.0.2.123")

        assertNull(device)
    }
}
