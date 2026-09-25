package co.sorsby.debugtoolkit.data.lan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetRangeTest {

    @Test
    fun `lists every other host address in a slash 24 network`() {
        val hosts = SubnetRange.hostAddresses("192.168.1.42", 24)

        assertEquals(253, hosts.size)
        assertTrue("192.168.1.1" in hosts)
        assertTrue("192.168.1.254" in hosts)
        assertFalse("192.168.1.42" in hosts)
        assertFalse("192.168.1.0" in hosts)
        assertFalse("192.168.1.255" in hosts)
    }

    @Test
    fun `lists hosts in a small slash 30 network`() {
        val hosts = SubnetRange.hostAddresses("10.0.0.1", 30)

        assertEquals(listOf("10.0.0.2"), hosts)
    }

    @Test
    fun `rejects a prefix length larger than the network can sweep`() {
        assertThrows(IllegalArgumentException::class.java) {
            SubnetRange.hostAddresses("10.0.0.1", 16)
        }
    }

    @Test
    fun `rejects a prefix length smaller than a single host network`() {
        assertThrows(IllegalArgumentException::class.java) {
            SubnetRange.hostAddresses("10.0.0.1", 31)
        }
    }

    @Test
    fun `formats the network address as CIDR`() {
        assertEquals("192.168.1.0/24", SubnetRange.cidr("192.168.1.42", 24))
    }

    @Test
    fun `rejects an address with the wrong number of parts`() {
        assertThrows(IllegalArgumentException::class.java) {
            SubnetRange.cidr("192.168.1", 24)
        }
    }

    @Test
    fun `rejects an address with an out of range octet`() {
        assertThrows(IllegalArgumentException::class.java) {
            SubnetRange.cidr("192.168.1.999", 24)
        }
    }

    @Test
    fun `sort key orders addresses numerically`() {
        val addresses = listOf("192.168.1.20", "192.168.1.2", "192.168.1.100")

        val sorted = addresses.sortedBy(SubnetRange::sortKey)

        assertEquals(listOf("192.168.1.2", "192.168.1.20", "192.168.1.100"), sorted)
    }
}
