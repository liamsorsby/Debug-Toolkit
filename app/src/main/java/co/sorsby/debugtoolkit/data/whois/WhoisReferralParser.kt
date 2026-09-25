package co.sorsby.debugtoolkit.data.whois

/**
 * Extracts the authoritative WHOIS server referral (the "refer:" field) that IANA's WHOIS server
 * returns when asked about a top level domain, so a second query can be sent straight to the
 * registry that actually holds the requested domain's records.
 */
object WhoisReferralParser {
    private val REFER_LINE = Regex("""(?im)^\s*refer:\s*(\S+)\s*$""")

    fun findReferral(response: String): String? =
        REFER_LINE.find(response)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}
