package co.sorsby.debugtoolkit.core.model

/** Which of the two related probes the Ping screen is currently configured to run. */
enum class PingMode {
    PING,
    TRACEROUTE,
}

/** One probe sent as part of a ping run. `roundTripMs` is null when that probe timed out. */
data class PingProbe(
    val sequence: Int,
    val roundTripMs: Double?,
)

data class PingResult(
    val host: String,
    val transmitted: Int,
    val received: Int,
    val packetLossPercent: Double,
    val probes: List<PingProbe>,
    val minMs: Double?,
    val avgMs: Double?,
    val maxMs: Double?,
    val jitterMs: Double,
)

/** One hop of a traceroute. `address` is null when that hop did not reply before the timeout. */
data class TracerouteHop(
    val hopNumber: Int,
    val address: String?,
    val roundTripMs: Double?,
)

data class TracerouteResult(
    val host: String,
    val hops: List<TracerouteHop>,
    val reachedDestination: Boolean,
)
