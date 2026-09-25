package co.sorsby.debugtoolkit.data.lan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkAddressSelectorTest {

    @Test
    fun `selects a 10 dot address`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("10.1.2.3" to 8))

        assertEquals("10.1.2.3" to 8, result)
    }

    @Test
    fun `selects a 192 168 address`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("192.168.1.5" to 24))

        assertEquals("192.168.1.5" to 24, result)
    }

    @Test
    fun `selects a 172 16 to 172 31 address`() {
        assertEquals(
            "172.16.0.5" to 24,
            LinkAddressSelector.selectPrivateIpv4(listOf("172.16.0.5" to 24)),
        )
        assertEquals(
            "172.31.0.5" to 24,
            LinkAddressSelector.selectPrivateIpv4(listOf("172.31.0.5" to 24)),
        )
    }

    @Test
    fun `does not select a public address`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("203.0.113.10" to 24))

        assertNull(result)
    }

    @Test
    fun `does not select a 172 32 address which is outside the private range`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("172.32.0.5" to 24))

        assertNull(result)
    }

    @Test
    fun `does not select a 192 address outside the 168 block`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("192.169.0.5" to 24))

        assertNull(result)
    }

    @Test
    fun `does not select an address with the wrong number of parts`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("10.0.0" to 24))

        assertNull(result)
    }

    @Test
    fun `does not select an address with an out of range octet`() {
        val result = LinkAddressSelector.selectPrivateIpv4(listOf("10.0.0.999" to 24))

        assertNull(result)
    }

    @Test
    fun `skips public addresses and picks the first private one`() {
        val result = LinkAddressSelector.selectPrivateIpv4(
            listOf("203.0.113.10" to 24, "192.168.1.5" to 24),
        )

        assertEquals("192.168.1.5" to 24, result)
    }

    @Test
    fun `returns null for an empty list`() {
        assertNull(LinkAddressSelector.selectPrivateIpv4(emptyList()))
    }
}
