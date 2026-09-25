package co.sorsby.debugtoolkit.core.model

/**
 * Result of a WHOIS lookup. WHOIS has no structured wire format, so [rawText] is the plain text
 * response exactly as the authoritative registry sent it; [server] records which server that
 * response actually came from, since a lookup may be redirected once from IANA to a registry.
 */
data class WhoisResult(
    val domain: String,
    val server: String,
    val rawText: String,
    val elapsedMs: Long,
)
