package co.sorsby.debugtoolkit.core.model

/** A single host that answered during a local network sweep. */
data class LanDevice(
    val ipAddress: String,
    val hostname: String?,
    val responseTimeMs: Long,
)

/** Result of sweeping the device's local subnet for other reachable hosts. */
data class LanScanResult(
    val subnetCidr: String,
    val devices: List<LanDevice>,
    val addressesScanned: Int,
    val elapsedMs: Long,
)
