package co.sorsby.debugtoolkit.data.ping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PingOutputParserTest {
    @Test
    fun `parses a fully successful run`() {
        val output = """
            PING example.com (93.184.216.34) 56(84) bytes of data.
            64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=11.2 ms
            64 bytes from 93.184.216.34: icmp_seq=2 ttl=56 time=10.8 ms
            64 bytes from 93.184.216.34: icmp_seq=3 ttl=56 time=12.0 ms

            --- example.com ping statistics ---
            3 packets transmitted, 3 received, 0% packet loss, time 2003ms
            rtt min/avg/max/mdev = 10.8/11.333/12.0/0.492 ms
        """.trimIndent()

        val result = PingOutputParser.parse("example.com", output)

        assertEquals(3, result.transmitted)
        assertEquals(3, result.received)
        assertEquals(0.0, result.packetLossPercent, 0.0)
        assertEquals(listOf(11.2, 10.8, 12.0), result.probes.map { it.roundTripMs })
        assertEquals(10.8, requireNotNull(result.minMs), 0.001)
        assertEquals(12.0, requireNotNull(result.maxMs), 0.001)
    }

    @Test
    fun `records a timed out probe as a null round trip`() {
        val output = """
            PING example.com (93.184.216.34) 56(84) bytes of data.
            64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=11.2 ms
            64 bytes from 93.184.216.34: icmp_seq=3 ttl=56 time=12.0 ms

            --- example.com ping statistics ---
            3 packets transmitted, 2 received, 33% packet loss, time 3003ms
        """.trimIndent()

        val result = PingOutputParser.parse("example.com", output)

        assertEquals(3, result.probes.size)
        assertEquals(11.2, requireNotNull(result.probes[0].roundTripMs), 0.001)
        assertNull(result.probes[1].roundTripMs)
        assertEquals(12.0, requireNotNull(result.probes[2].roundTripMs), 0.001)
        assertEquals(33.0, result.packetLossPercent, 0.0)
    }

    @Test
    fun `throws when the summary line is missing`() {
        assertThrows(IllegalStateException::class.java) {
            PingOutputParser.parse("example.com", "ping: unknown host example.com")
        }
    }

    @Test
    fun `reports null statistics when every probe timed out`() {
        val output = """
            PING example.com (93.184.216.34) 56(84) bytes of data.

            --- example.com ping statistics ---
            2 packets transmitted, 0 received, 100% packet loss, time 2003ms
        """.trimIndent()

        val result = PingOutputParser.parse("example.com", output)

        assertEquals(2, result.probes.size)
        assertEquals(true, result.probes.all { it.roundTripMs == null })
        assertNull(result.minMs)
        assertNull(result.avgMs)
        assertNull(result.maxMs)
        assertEquals(0.0, result.jitterMs, 0.0)
    }
}
