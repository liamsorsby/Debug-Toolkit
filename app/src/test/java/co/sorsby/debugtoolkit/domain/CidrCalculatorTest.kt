package co.sorsby.debugtoolkit.domain

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CidrCalculatorTest {
    @Test
    fun calculatesAStandardBlockFromAHostAddress() {
        val block = CidrCalculator.calculate("192.168.1.10/24")

        assertEquals("192.168.1.0/24", block.cidr)
        assertEquals("192.168.1.0", block.networkAddress)
        assertEquals("192.168.1.255", block.broadcastAddress)
        assertEquals("255.255.255.0", block.subnetMask)
        assertEquals("0.0.0.255", block.wildcardMask)
        assertEquals("192.168.1.1", block.firstHost)
        assertEquals("192.168.1.254", block.lastHost)
        assertEquals(256, block.totalAddresses)
        assertEquals(254, block.usableHosts)
    }

    @Test
    fun calculatesAnUnalignedPrefix() {
        val block = CidrCalculator.calculate("10.1.2.200/21")

        assertEquals("10.1.0.0/21", block.cidr)
        assertEquals("10.1.7.255", block.broadcastAddress)
        assertEquals("255.255.248.0", block.subnetMask)
        assertEquals(2046, block.usableHosts)
    }

    @Test
    fun treatsSlash31AsATwoHostLinkWithNoBroadcast() {
        val block = CidrCalculator.calculate("10.0.0.5/31")

        assertEquals("10.0.0.4", block.firstHost)
        assertEquals("10.0.0.5", block.lastHost)
        assertNull(block.broadcastAddress)
        assertEquals(2, block.usableHosts)
    }

    @Test
    fun treatsSlash32AsASingleHost() {
        val block = CidrCalculator.calculate("203.0.113.9/32")

        assertEquals("203.0.113.9", block.firstHost)
        assertEquals("203.0.113.9", block.lastHost)
        assertNull(block.broadcastAddress)
        assertEquals("255.255.255.255", block.subnetMask)
        assertEquals(1, block.totalAddresses)
        assertEquals(1, block.usableHosts)
    }

    @Test
    fun handlesTheWholeAddressSpace() {
        val block = CidrCalculator.calculate("0.0.0.0/0")

        assertEquals("0.0.0.0", block.subnetMask)
        assertEquals("255.255.255.255", block.wildcardMask)
        assertEquals("255.255.255.255", block.broadcastAddress)
        assertEquals(4_294_967_296L, block.totalAddresses)
        assertEquals(4_294_967_294L, block.usableHosts)
    }

    @Test
    fun ignoresSurroundingWhitespace() {
        assertEquals("172.16.0.0/12", CidrCalculator.calculate("  172.20.1.1/12 ").cidr)
    }

    @Test
    fun rejectsMalformedInput() {
        listOf(
            "", "192.168.1.1", "192.168.1/24", "192.168.1.1/", "192.168.1.1/ab",
            "192.168.1.1/24/8", "::1/64", "192.168.1.1 /24",
        ).forEach { input ->
            assertThrows(input, IllegalArgumentException::class.java) {
                CidrCalculator.calculate(input)
            }
        }
    }

    @Test
    fun rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException::class.java) { CidrCalculator.calculate("256.0.0.1/24") }
        assertThrows(IllegalArgumentException::class.java) { CidrCalculator.calculate("10.0.0.1/33") }
    }

    @Test
    fun calculatesAnIpv6Block() {
        val block = CidrCalculator.calculateIpv6("2001:0DB8:abcd::1/32")

        assertEquals("2001:db8::/32", block.cidr)
        assertEquals("2001:db8::", block.networkAddress)
        assertEquals("2001:db8:ffff:ffff:ffff:ffff:ffff:ffff", block.lastAddress)
        assertEquals("ffff:ffff::", block.subnetMask)
        assertEquals(BigInteger.ONE.shiftLeft(96), block.totalAddresses)
        assertEquals(BigInteger.ONE.shiftLeft(32), block.subnets64)
    }

    @Test
    fun calculatesIpv6BlocksAtTheEdges() {
        val everything = CidrCalculator.calculateIpv6("::/0")
        assertEquals("::/0", everything.cidr)
        assertEquals("::", everything.subnetMask)
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", everything.lastAddress)
        assertEquals(BigInteger.ONE.shiftLeft(128), everything.totalAddresses)
        assertEquals(BigInteger.ONE.shiftLeft(64), everything.subnets64)

        val single = CidrCalculator.calculateIpv6("2001:db8::1/128")
        assertEquals("2001:db8::1", single.networkAddress)
        assertEquals("2001:db8::1", single.lastAddress)
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", single.subnetMask)
        assertEquals(BigInteger.ONE, single.totalAddresses)
        assertNull(single.subnets64)
        assertEquals(BigInteger.ONE, CidrCalculator.calculateIpv6("2001:db8::/64").subnets64)
    }

    @Test
    fun masksAnUnalignedIpv6Prefix() {
        val block = CidrCalculator.calculateIpv6("fe80::1/10")

        assertEquals("fe80::/10", block.cidr)
        assertEquals("ffc0::", block.subnetMask)
        assertEquals("febf:ffff:ffff:ffff:ffff:ffff:ffff:ffff", block.lastAddress)
    }

    @Test
    fun compressesTheLongestRunOfZeroGroupsOnly() {
        assertEquals("1:0:0:2::3", CidrCalculator.calculateIpv6("1:0:0:2:0:0:0:3/128").cidr.substringBefore("/"))
        assertEquals("1:0:2:3:4:5:6:7", CidrCalculator.calculateIpv6("1:0:2:3:4:5:6:7/128").networkAddress)
    }

    @Test
    fun choosesTheFamilyFromTheInput() {
        assertTrue(CidrCalculator.calculateAny("10.0.0.0/8") is CidrResult.V4)
        assertTrue(CidrCalculator.calculateAny("2001:db8::/32") is CidrResult.V6)
    }

    @Test
    fun rejectsMalformedIpv6() {
        listOf(
            "2001:db8::", "2001:db8::/129", "2001:db8::/", "2001:db8:::1/64", "gggg::/64",
            "::ffff:1.2.3.4/96", "2001:db8::%eth0/64", "12345::/16", "1:2:3:4:5:6:7:8:9/64",
        ).forEach { input ->
            assertThrows(input, IllegalArgumentException::class.java) {
                CidrCalculator.calculateIpv6(input)
            }
        }
    }
}
