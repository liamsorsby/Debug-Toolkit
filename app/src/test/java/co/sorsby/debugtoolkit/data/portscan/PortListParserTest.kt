package co.sorsby.debugtoolkit.data.portscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PortListParserTest {
    @Test
    fun `parses individual ports in order without duplicates`() {
        assertEquals(listOf(22, 80, 443), PortListParser.parse("443,22,80,22"))
    }

    @Test
    fun `expands a range into individual ports`() {
        assertEquals(listOf(8000, 8001, 8002), PortListParser.parse("8000-8002"))
    }

    @Test
    fun `merges overlapping ranges and singles`() {
        assertEquals(listOf(20, 21, 22, 23), PortListParser.parse("20-22,21-23"))
    }

    @Test
    fun `trims whitespace around tokens`() {
        assertEquals(listOf(22, 80), PortListParser.parse(" 22 , 80 "))
    }

    @Test
    fun `rejects blank input`() {
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("") }
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("   ") }
    }

    @Test
    fun `rejects a value outside the valid port range`() {
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("0") }
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("65536") }
    }

    @Test
    fun `rejects a malformed token`() {
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("abc") }
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("1-2-3") }
    }

    @Test
    fun `rejects a range whose start is after its end`() {
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse("100-50") }
    }

    @Test
    fun `rejects a single range or total selection larger than the cap`() {
        assertThrows(IllegalArgumentException::class.java) {
            PortListParser.parse("1-${PortListParser.MAX_PORTS + 1}")
        }
        val manyPorts = (1..PortListParser.MAX_PORTS + 1).joinToString(",")
        assertThrows(IllegalArgumentException::class.java) { PortListParser.parse(manyPorts) }
    }

    @Test
    fun `common ports preset is within the cap`() {
        assertEquals(true, PortListParser.COMMON_PORTS.size <= PortListParser.MAX_PORTS)
        assertEquals(PortListParser.COMMON_PORTS.sorted(), PortListParser.COMMON_PORTS)
    }
}
