package co.sorsby.debugtoolkit.data.ping

import co.sorsby.debugtoolkit.core.model.TracerouteResult
import co.sorsby.debugtoolkit.domain.InputValidation

interface TracerouteRunner {
    suspend fun traceroute(host: String, maxHops: Int): TracerouteResult
}

/**
 * Sends one probe per hop by shelling out to `ping -t <ttl>`, raising the outgoing TTL by one
 * each time and reading whether the reply came from an intermediate router ("Time to live
 * exceeded") or the destination itself. This relies entirely on the same unprivileged ping
 * socket mechanism as [ShellPingRunner], reusing the OS's own `ping` binary rather than
 * hand-rolling ICMP packet parsing, which keeps this portable across Android versions at the
 * cost of depending on that binary's text output staying in a recognisable shape. Behaviour
 * varies by device and network; a hop that never replies is reported, not treated as failure,
 * and the whole probe fails clearly rather than hanging if the binary cannot be run at all.
 */
class ShellTracerouteRunner(
    private val processRunner: ProcessRunner = SystemProcessRunner(),
) : TracerouteRunner {
    override suspend fun traceroute(host: String, maxHops: Int): TracerouteResult {
        val target = InputValidation.host(host)
        require(maxHops in 1..MAX_HOP_LIMIT) { "Trace at most $MAX_HOP_LIMIT hops." }

        val hops = mutableListOf<co.sorsby.debugtoolkit.core.model.TracerouteHop>()
        var reached = false
        for (ttl in 1..maxHops) {
            val result = processRunner.run(
                command = listOf(
                    "/system/bin/ping",
                    "-c",
                    "1",
                    "-t",
                    ttl.toString(),
                    "-W",
                    PER_HOP_TIMEOUT_SECONDS.toString(),
                    target,
                ),
                timeoutSeconds = PER_HOP_TIMEOUT_SECONDS + BASE_TIMEOUT_SECONDS,
            )
            val probe = TracerouteOutputParser.parseHop(ttl, result.output)
            hops += probe.hop
            if (probe.reachedDestination) {
                reached = true
                break
            }
        }
        return TracerouteResult(host = target, hops = hops, reachedDestination = reached)
    }

    private companion object {
        const val MAX_HOP_LIMIT = 30
        const val PER_HOP_TIMEOUT_SECONDS = 2L
        const val BASE_TIMEOUT_SECONDS = 3L
    }
}
