package co.sorsby.debugtoolkit.data.publicip

/**
 * Parses the plain text key=value response from Cloudflare's `/cdn-cgi/trace` endpoint, for
 * example:
 * fl=1f1
 * ip=203.0.113.10
 * loc=GB
 * colo=LHR
 * warp=off
 */
object CloudflareTraceParser {
    fun parse(body: String): Map<String, String> = body
        .lineSequence()
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }
        .toMap()
}
