package co.sorsby.debugtoolkit.data.lan

/**
 * Picks which of the device's link addresses to sweep. Only private (RFC 1918) IPv4 addresses
 * are useful subnets to scan for other on-LAN devices; public and link-local addresses are
 * skipped.
 */
object LinkAddressSelector {
    fun selectPrivateIpv4(addresses: List<Pair<String, Int>>): Pair<String, Int>? =
        addresses.firstOrNull { (address, _) -> isPrivateIpv4(address) }

    private fun isPrivateIpv4(address: String): Boolean {
        val parts = address.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        return when (parts[0]) {
            10 -> true
            172 -> parts[1] in 16..31
            192 -> parts[1] == 168
            else -> false
        }
    }
}
