package co.sorsby.debugtoolkit.data.ping

import co.sorsby.debugtoolkit.core.model.PingResult
import co.sorsby.debugtoolkit.domain.InputValidation

interface PingRunner {
    suspend fun ping(host: String, count: Int): PingResult
}

/**
 * Shells out to the `ping` binary that ships with Android. This is the same unprivileged
 * mechanism every Android app can use without root: the OS grants `AID_INET` group access to
 * the kernel's ICMP "ping socket", and `/system/bin/ping` is simply the reference client for
 * it. No custom socket handling is done here; the binary's own text output is parsed instead.
 */
class ShellPingRunner(
    private val processRunner: ProcessRunner = SystemProcessRunner(),
) : PingRunner {
    override suspend fun ping(host: String, count: Int): PingResult {
        val target = InputValidation.host(host)
        require(count in 1..MAX_PROBES) { "Send between 1 and $MAX_PROBES probes." }
        val result = processRunner.run(
            command = listOf("/system/bin/ping", "-c", count.toString(), target),
            timeoutSeconds = count.toLong() * PER_PROBE_TIMEOUT_SECONDS + BASE_TIMEOUT_SECONDS,
        )
        return PingOutputParser.parse(target, result.output)
    }

    private companion object {
        const val MAX_PROBES = 10
        const val PER_PROBE_TIMEOUT_SECONDS = 2L
        const val BASE_TIMEOUT_SECONDS = 5L
    }
}
