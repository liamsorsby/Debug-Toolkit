package co.sorsby.debugtoolkit.domain

import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

data class CidrBlock(
    val cidr: String,
    val prefixLength: Int,
    val networkAddress: String,
    /** Null for /31 and /32 blocks, which have no broadcast address (RFC 3021). */
    val broadcastAddress: String?,
    val subnetMask: String,
    val wildcardMask: String,
    val firstHost: String,
    val lastHost: String,
    val totalAddresses: Long,
    val usableHosts: Long,
)

data class Ipv6Block(
    val cidr: String,
    val prefixLength: Int,
    val networkAddress: String,
    /** IPv6 has no broadcast address, so this is simply the last address in the block. */
    val lastAddress: String,
    val subnetMask: String,
    val totalAddresses: BigInteger,
    /** How many /64 subnets fit in the block, or null when the block is smaller than a /64. */
    val subnets64: BigInteger?,
)

sealed interface CidrResult {
    data class V4(val block: CidrBlock) : CidrResult
    data class V6(val block: Ipv6Block) : CidrResult
}

/** Pure IPv4 and IPv6 CIDR arithmetic, with no cap on the block size. */
object CidrCalculator {
    private val IPV6_INPUT = Regex("""^([0-9a-fA-F:.]+)/(\d{1,3})$""")
    private val ALL_BITS = BigInteger.ONE.shiftLeft(128) - BigInteger.ONE

    /** Chooses IPv6 when the input contains a colon, otherwise IPv4. */
    fun calculateAny(input: String): CidrResult =
        if (':' in input) CidrResult.V6(calculateIpv6(input)) else CidrResult.V4(calculate(input))

    fun calculateIpv6(input: String): Ipv6Block {
        val match = requireNotNull(IPV6_INPUT.matchEntire(input.trim())) { IPV6_ERROR }
        val prefixLength = match.groupValues[2].toInt()
        require(prefixLength in 0..128) { IPV6_ERROR }
        val address = try {
            InetAddress.getByName(match.groupValues[1])
        } catch (_: UnknownHostException) {
            throw IllegalArgumentException(IPV6_ERROR)
        }
        // IPv4-mapped literals such as ::ffff:1.2.3.4 come back as IPv4 addresses.
        require(address is Inet6Address) { IPV6_ERROR }

        val hostBits = 128 - prefixLength
        val mask = ALL_BITS xor BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE)
        val network = BigInteger(1, address.address) and mask
        val total = BigInteger.ONE.shiftLeft(hostBits)
        return Ipv6Block(
            cidr = "${format(network)}/$prefixLength",
            prefixLength = prefixLength,
            networkAddress = format(network),
            lastAddress = format(network + total - BigInteger.ONE),
            subnetMask = format(mask),
            totalAddresses = total,
            subnets64 = if (prefixLength <= 64) BigInteger.ONE.shiftLeft(64 - prefixLength) else null,
        )
    }

    // RFC 5952: lower case, with the longest run of two or more zero groups collapsed to "::".
    private fun format(value: BigInteger): String {
        val groups = (7 downTo 0).map { value.shiftRight(it * 16).toInt() and 0xFFFF }
        var bestStart = -1
        var bestLength = 0
        var index = 0
        while (index < groups.size) {
            if (groups[index] != 0) {
                index++
                continue
            }
            val start = index
            while (index < groups.size && groups[index] == 0) index++
            if (index - start > bestLength) {
                bestStart = start
                bestLength = index - start
            }
        }
        if (bestLength < 2) return groups.joinToString(":") { it.toString(16) }
        val head = groups.take(bestStart).joinToString(":") { it.toString(16) }
        val tail = groups.drop(bestStart + bestLength).joinToString(":") { it.toString(16) }
        return "$head::$tail"
    }

    private const val IPV6_ERROR =
        "Enter an IPv6 address and a prefix from 0 to 128, such as 2001:db8::/32."

    private val CIDR_INPUT = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})/(\d{1,2})$""")

    fun calculate(input: String): CidrBlock {
        val match = requireNotNull(CIDR_INPUT.matchEntire(input.trim())) {
            "Enter an IPv4 address and prefix, such as 192.168.1.0/24."
        }
        val octets = match.groupValues.subList(1, 5).map { it.toInt() }
        val prefixLength = match.groupValues[5].toInt()
        require(octets.all { it in 0..255 } && prefixLength in 0..32) {
            "Octets must be 0-255 and the prefix length 0-32."
        }

        val address = octets.fold(0L) { acc, octet -> (acc shl 8) or octet.toLong() }
        val mask = if (prefixLength == 0) 0L else (0xFFFFFFFFL shl (32 - prefixLength)) and 0xFFFFFFFFL
        val network = address and mask
        val broadcast = network or (mask xor 0xFFFFFFFFL)
        val total = broadcast - network + 1

        val hasHostRange = prefixLength <= 30
        return CidrBlock(
            cidr = "${dotted(network)}/$prefixLength",
            prefixLength = prefixLength,
            networkAddress = dotted(network),
            broadcastAddress = if (hasHostRange) dotted(broadcast) else null,
            subnetMask = dotted(mask),
            wildcardMask = dotted(mask xor 0xFFFFFFFFL),
            firstHost = dotted(if (hasHostRange) network + 1 else network),
            lastHost = dotted(if (hasHostRange) broadcast - 1 else broadcast),
            totalAddresses = total,
            usableHosts = if (hasHostRange) total - 2 else total,
        )
    }

    private fun dotted(value: Long): String =
        "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
}
