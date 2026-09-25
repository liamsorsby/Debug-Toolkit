package co.sorsby.debugtoolkit.data.lan

/**
 * Pure IPv4 subnet arithmetic for a local network sweep: given the device's own address and
 * network prefix length, works out every other host address in that subnet, and the CIDR
 * notation to display for it. Networks are capped to /24 or smaller (254 hosts or fewer) so a
 * sweep always completes in a reasonable time and cannot be pointed at an enormous range.
 */
object SubnetRange {
    const val MIN_PREFIX_LENGTH = 24
    const val MAX_PREFIX_LENGTH = 30

    /** Every other host address in the subnet, excluding the network address, the broadcast
     * address, and the device's own address, in ascending order. */
    fun hostAddresses(address: String, prefixLength: Int): List<String> {
        require(prefixLength in MIN_PREFIX_LENGTH..MAX_PREFIX_LENGTH) {
            "Prefix length must be between $MIN_PREFIX_LENGTH and $MAX_PREFIX_LENGTH."
        }
        val addressInt = toInt(address)
        val mask = maskFor(prefixLength)
        val network = addressInt and mask
        val broadcast = network or mask.inv()
        return ((network + 1) until broadcast)
            .map(::toDottedString)
            .filter { it != address }
    }

    /** The subnet in CIDR notation, for example 192.168.1.0/24. */
    fun cidr(address: String, prefixLength: Int): String {
        val mask = maskFor(prefixLength)
        val network = toInt(address) and mask
        return "${toDottedString(network)}/$prefixLength"
    }

    /** A numeric sort key so scan results can be shown in ascending IP order. */
    fun sortKey(address: String): Long = toInt(address).toLong() and 0xFFFFFFFFL

    private fun maskFor(prefixLength: Int): Int = -1 shl (32 - prefixLength)

    private fun toInt(address: String): Int {
        val parts = address.split(".").map { it.toInt() }
        require(parts.size == 4 && parts.all { it in 0..255 }) { "Invalid IPv4 address." }
        return (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
    }

    private fun toDottedString(value: Int): String =
        "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}.${(value ushr 8) and 0xFF}.${value and 0xFF}"
}
