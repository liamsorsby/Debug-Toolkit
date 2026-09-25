package co.sorsby.debugtoolkit.data.ping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TracerouteOutputParserTest {
    @Test
    fun `recognises a destination echo reply`() {
        val output = """
            PING example.com (93.184.216.34) 56(84) bytes of data.
            64 bytes from 93.184.216.34: icmp_seq=1 ttl=12 time=11.2 ms
        """.trimIndent()

        val probe = TracerouteOutputParser.parseHop(5, output)

        assertTrue(probe.reachedDestination)
        assertEquals(5, probe.hop.hopNumber)
        assertEquals("93.184.216.34", probe.hop.address)
        assertEquals(11.2, requireNotNull(probe.hop.roundTripMs), 0.001)
    }

    @Test
    fun `recognises an intermediate router time exceeded reply`() {
        val output = """
            PING example.com (93.184.216.34) 56(84) bytes of data.
            From 192.168.1.1 icmp_seq=1 Time to live exceeded
        """.trimIndent()

        val probe = TracerouteOutputParser.parseHop(2, output)

        assertFalse(probe.reachedDestination)
        assertEquals("192.168.1.1", probe.hop.address)
        assertNull(probe.hop.roundTripMs)
    }

    @Test
    fun `reports a hop that never replied as unknown`() {
        val output = """
            PING example.com (93.184.216.34) 56(84) bytes of data.

            --- example.com ping statistics ---
            1 packets transmitted, 0 received, 100% packet loss, time 1002ms
        """.trimIndent()

        val probe = TracerouteOutputParser.parseHop(3, output)

        assertFalse(probe.reachedDestination)
        assertNull(probe.hop.address)
        assertNull(probe.hop.roundTripMs)
    }
}
