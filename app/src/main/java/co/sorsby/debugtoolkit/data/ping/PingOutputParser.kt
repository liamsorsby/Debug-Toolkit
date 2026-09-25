package co.sorsby.debugtoolkit.data.ping

import co.sorsby.debugtoolkit.core.model.PingProbe
import co.sorsby.debugtoolkit.core.model.PingResult
import co.sorsby.debugtoolkit.domain.SpeedMath

/**
 * Turns the text `/system/bin/ping` prints into [PingResult]. Kept as pure functions, separate
 * from [ShellPingRunner], so the parsing logic is fully unit testable without a real `ping`
 * binary or process.
 */
object PingOutputParser {
    private val PROBE_LINE = Regex("""icmp_seq=(\d+).*?time[=<]([\d.]+)""")
    private val SUMMARY_LINE = Regex(
        """(\d+) packets transmitted, (\d+) (?:packets )?received,.*?([\d.]+)% packet loss""",
    )

    fun parse(host: String, output: String): PingResult {
        val summary = SUMMARY_LINE.find(output)
            ?: throw IllegalStateException("Unable to read ping results for $host.")
        val (transmittedText, receivedText, lossText) = summary.destructured
        val transmitted = transmittedText.toInt()
        val received = receivedText.toInt()

        val latenciesBySequence = PROBE_LINE.findAll(output).associate { match ->
            val (sequenceText, timeText) = match.destructured
            sequenceText.toInt() to timeText.toDouble()
        }
        val probes = (1..transmitted).map { sequence ->
            PingProbe(sequence = sequence, roundTripMs = latenciesBySequence[sequence])
        }
        val latencies = probes.mapNotNull { it.roundTripMs }

        return PingResult(
            host = host,
            transmitted = transmitted,
            received = received,
            packetLossPercent = lossText.toDouble(),
            probes = probes,
            minMs = latencies.minOrNull(),
            avgMs = latencies.takeIf { it.isNotEmpty() }?.average(),
            maxMs = latencies.maxOrNull(),
            jitterMs = SpeedMath.jitterMs(latencies),
        )
    }
}
