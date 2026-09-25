package co.sorsby.debugtoolkit.data.whois

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhoisReferralParserTest {

    @Test
    fun `finds a refer line among other fields`() {
        val response = """
            % IANA WHOIS server
            domain: COM

            organisation: VeriSign Global Registry Services

            refer:        whois.verisign-grs.com

            whois:        whois.verisign-grs.com
        """.trimIndent()

        assertEquals("whois.verisign-grs.com", WhoisReferralParser.findReferral(response))
    }

    @Test
    fun `is case insensitive`() {
        val response = "REFER: whois.nic.example\n"

        assertEquals("whois.nic.example", WhoisReferralParser.findReferral(response))
    }

    @Test
    fun `returns null when there is no refer line`() {
        val response = "domain: EXAMPLE.COM\nstatus: ACTIVE\n"

        assertNull(WhoisReferralParser.findReferral(response))
    }

    @Test
    fun `ignores a refer line with no value`() {
        val response = "refer:\ndomain: EXAMPLE\n"

        assertNull(WhoisReferralParser.findReferral(response))
    }
}
