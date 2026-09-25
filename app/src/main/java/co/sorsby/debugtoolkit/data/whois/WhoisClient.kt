package co.sorsby.debugtoolkit.data.whois

import co.sorsby.debugtoolkit.core.model.WhoisResult
import co.sorsby.debugtoolkit.domain.InputValidation
import kotlin.time.TimeSource

interface WhoisClient {
    suspend fun lookup(domain: String): WhoisResult
}

/**
 * Looks up WHOIS records for a domain name. IANA's WHOIS server knows which registry is
 * authoritative for every top level domain but does not hold per-domain registration records
 * itself, so a lookup is a single referral hop: query IANA for the domain, read the "refer:"
 * line it sends back, then query that registry directly for the real record. If IANA gives no
 * referral (or refers back to itself) its own response is returned as-is.
 */
class SocketWhoisClient(
    private val transport: WhoisTransport = SocketWhoisTransport(),
    private val ianaServer: String = "whois.iana.org",
    private val port: Int = 43,
) : WhoisClient {

    override suspend fun lookup(domain: String): WhoisResult {
        val validated = InputValidation.domain(domain)
        val mark = TimeSource.Monotonic.markNow()
        val ianaResponse = transport.query(ianaServer, port, validated)
        val referral = WhoisReferralParser.findReferral(ianaResponse)
        val (server, text) = if (referral != null && !referral.equals(ianaServer, ignoreCase = true)) {
            referral to transport.query(referral, port, validated)
        } else {
            ianaServer to ianaResponse
        }
        return WhoisResult(
            domain = validated,
            server = server,
            rawText = text,
            elapsedMs = mark.elapsedNow().inWholeMilliseconds,
        )
    }
}
