package co.sorsby.debugtoolkit.data.dns

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies every [DnsRecordType] resolves to its IANA-assigned numeric RRTYPE.
 * These codes are sent verbatim to the DNS-over-HTTPS resolver, so a typo here
 * would silently query the wrong record type without failing any other test.
 */
class DnsRecordTypeTest {

    @Test
    fun `record types map to their IANA assigned codes`() {
        val expectedCodes = mapOf(
            DnsRecordType.A to 1,
            DnsRecordType.NS to 2,
            DnsRecordType.CNAME to 5,
            DnsRecordType.SOA to 6,
            DnsRecordType.PTR to 12,
            DnsRecordType.MX to 15,
            DnsRecordType.TXT to 16,
            DnsRecordType.RP to 17,
            DnsRecordType.AFSDB to 18,
            DnsRecordType.AAAA to 28,
            DnsRecordType.LOC to 29,
            DnsRecordType.SRV to 33,
            DnsRecordType.NAPTR to 35,
            DnsRecordType.CERT to 37,
            DnsRecordType.DNAME to 39,
            DnsRecordType.APL to 42,
            DnsRecordType.SSHFP to 44,
            DnsRecordType.IPSECKEY to 45,
            DnsRecordType.RRSIG to 46,
            DnsRecordType.NSEC to 47,
            DnsRecordType.DNSKEY to 48,
            DnsRecordType.DHCID to 49,
            DnsRecordType.HIP to 55,
            DnsRecordType.CDNSKEY to 60,
            DnsRecordType.CAA to 257,
        )

        expectedCodes.forEach { (type, expectedCode) ->
            assertEquals("${type.name} should map to RRTYPE $expectedCode", expectedCode, type.code)
        }
    }

    @Test
    fun `every enum entry has a documented expected code`() {
        val documented = setOf(
            DnsRecordType.A, DnsRecordType.NS, DnsRecordType.CNAME, DnsRecordType.SOA,
            DnsRecordType.PTR, DnsRecordType.MX, DnsRecordType.TXT, DnsRecordType.RP,
            DnsRecordType.AFSDB, DnsRecordType.AAAA, DnsRecordType.LOC, DnsRecordType.SRV,
            DnsRecordType.NAPTR, DnsRecordType.CERT, DnsRecordType.DNAME, DnsRecordType.APL,
            DnsRecordType.SSHFP, DnsRecordType.IPSECKEY, DnsRecordType.RRSIG, DnsRecordType.NSEC,
            DnsRecordType.DNSKEY, DnsRecordType.DHCID, DnsRecordType.HIP, DnsRecordType.CDNSKEY,
            DnsRecordType.CAA,
        )

        assertEquals(
            "A newly added DnsRecordType is missing its expected-code assertion above",
            DnsRecordType.entries.toSet(),
            documented,
        )
    }

    @Test
    fun `no two record types share the same code`() {
        val codes = DnsRecordType.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }
}
