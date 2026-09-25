package co.sorsby.debugtoolkit.data.ping

import co.sorsby.debugtoolkit.core.model.TracerouteHop

/** The parsed outcome of one traceroute probe, sent at a single TTL value. */
data class HopProbeResult(
    val hop: TracerouteHop,
    val reachedDestination: Boolean,
)

/**
 * Turns the text a single `ping -t <ttl> -c 1` invocation prints into a [HopProbeResult]. A
 * router that is not the final destination replies with an ICMP "Time to live exceeded"
 * message; the destination itself replies with a normal echo reply. Kept as pure functions,
 * separate from [ShellTracerouteRunner], so the parsing logic is fully unit testable.
 */
object TracerouteOutputParser {
    private val DESTINATION_REPLY = Regex("""bytes from ([^\s:]+).*?time[=<]([\d.]+)""")
    private val TIME_EXCEEDED = Regex(
        """[Ff]rom ([^\s:]+).*?Time to live exceeded""",
    )

    fun parseHop(hopNumber: Int, output: String): HopProbeResult {
        DESTINATION_REPLY.find(output)?.let { match ->
            val (address, timeText) = match.destructured
            return HopProbeResult(
                hop = TracerouteHop(hopNumber, address, timeText.toDouble()),
                reachedDestination = true,
            )
        }
        TIME_EXCEEDED.find(output)?.let { match ->
            val address = match.groupValues[1]
            return HopProbeResult(
                hop = TracerouteHop(hopNumber, address, roundTripMs = null),
                reachedDestination = false,
            )
        }
        return HopProbeResult(
            hop = TracerouteHop(hopNumber, address = null, roundTripMs = null),
            reachedDestination = false,
        )
    }
}
