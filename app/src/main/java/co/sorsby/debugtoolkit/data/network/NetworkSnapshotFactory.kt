package co.sorsby.debugtoolkit.data.network

import co.sorsby.debugtoolkit.core.model.NetworkSnapshot
import co.sorsby.debugtoolkit.core.model.NetworkTransport
import co.sorsby.debugtoolkit.domain.SignalQuality

/**
 * Turns the raw values read off the platform into the [NetworkSnapshot] the Network screen
 * renders. Deliberately free of Android types so every rule here is unit testable, leaving
 * [AndroidNetworkMonitor] as the thin layer that only reads those values, matching the split
 * between [co.sorsby.debugtoolkit.data.lan.LinkAddressSelector] and its own reader.
 */
object NetworkSnapshotFactory {
    /**
     * The value Wi-Fi reports when it has no usable measurement. Android documents this as
     * `WifiInfo.INVALID_RSSI`, and it must not be shown as if it were a real reading.
     */
    const val INVALID_RSSI_DBM = -127

    /** The snapshot for a device with no active or usable network. */
    fun disconnected(sampledAtEpochMillis: Long): NetworkSnapshot =
        NetworkSnapshot(sampledAtEpochMillis = sampledAtEpochMillis)

    fun connected(
        sampledAtEpochMillis: Long,
        transports: Set<NetworkTransport>,
        validated: Boolean,
        notMetered: Boolean,
        localAddresses: List<String>,
        wifiRssiDbm: Int?,
        linkDownstreamKbps: Int,
        linkUpstreamKbps: Int,
    ): NetworkSnapshot {
        val rssi = usableRssi(wifiRssiDbm)
        return NetworkSnapshot(
            sampledAtEpochMillis = sampledAtEpochMillis,
            connected = true,
            validated = validated,
            metered = !notMetered,
            transports = transports,
            localAddresses = localAddresses.filter(String::isNotEmpty),
            wifiRssiDbm = rssi,
            wifiSignal = SignalQuality.fromRssi(rssi),
            estimatedDownstreamKbps = positiveKbps(linkDownstreamKbps),
            estimatedUpstreamKbps = positiveKbps(linkUpstreamKbps),
        )
    }

    /** Drops the sentinel the platform reports when no real signal measurement is available. */
    fun usableRssi(rssiDbm: Int?): Int? = rssiDbm?.takeUnless { it <= INVALID_RSSI_DBM }

    /** Android reports an unknown link bandwidth as zero, which is not worth showing. */
    fun positiveKbps(kbps: Int): Int? = kbps.takeIf { it > 0 }
}
