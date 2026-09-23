package co.sorsby.debugtoolkit.core.model

data class NetworkSnapshot(
    val sampledAtEpochMillis: Long = 0,
    val connected: Boolean = false,
    val validated: Boolean = false,
    val metered: Boolean = false,
    val transports: Set<NetworkTransport> = emptySet(),
    val localAddresses: List<String> = emptyList(),
    val wifiRssiDbm: Int? = null,
    val wifiSignal: WifiSignal = WifiSignal.UNAVAILABLE,
    val linkDownKbps: Int? = null,
    val linkUpKbps: Int? = null,
)
