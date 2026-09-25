package co.sorsby.debugtoolkit.data.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsMessageCodecTest {

    @Test
    fun `encoded query carries the requested id name type and recursion flag`() {
        val query = DnsMessageCodec.encodeQuery(
            id = 0x1234,
            name = "example.com",
            type = DnsRecordType.A.code,
            recursionDesired = false,
        )

        // ID (2 bytes)
        assertEquals(0x12, query[0].toInt() and 0xFF)
        assertEquals(0x34, query[1].toInt() and 0xFF)
        // Flags: recursion desired bit must be clear for a non-recursive query.
        assertEquals(0, query[2].toInt() and 0x01)
        // QDCOUNT = 1, ARCOUNT = 1 (the EDNS0 OPT record)
        assertEquals(1, ((query[4].toInt() and 0xFF) shl 8) or (query[5].toInt() and 0xFF))
        assertEquals(1, ((query[10].toInt() and 0xFF) shl 8) or (query[11].toInt() and 0xFF))
        // QNAME starts at byte 12 as a length-prefixed label.
        assertEquals(7, query[12].toInt() and 0xFF)
        assertEquals('e'.code, query[13].toInt())
    }

    @Test
    fun `sets the recursion desired bit when requested`() {
        val query = DnsMessageCodec.encodeQuery(
            id = 1,
            name = "example.com",
            type = DnsRecordType.A.code,
            recursionDesired = true,
        )

        assertEquals(1, query[2].toInt() and 0x01)
    }

    @Test
    fun `encodes the root name as a single zero label`() {
        val query = DnsMessageCodec.encodeQuery(
            id = 1,
            name = ".",
            type = DnsRecordType.NS.code,
            recursionDesired = false,
        )

        // QNAME starts at byte 12; a root name is a lone zero-length label.
        assertEquals(0, query[12].toInt())
    }

    @Test
    fun `decodes a header and A record answer`() {
        val message = buildResponse(
            id = 0xABCD,
            flags = FLAG_QR or FLAG_RA,
            answers = listOf(encodeName("example.com") + typeClassTtlRdata(1, byteArrayOf(192.toByte(), 0, 2, 1))),
        )

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals(0xABCD, decoded.id)
        assertTrue(decoded.recursionAvailable)
        assertFalse(decoded.authoritative)
        assertFalse(decoded.truncated)
        assertEquals(0, decoded.responseCode)
        assertEquals("192.0.2.1", decoded.answers.single().rdata)
        assertEquals("example.com.", decoded.answers.single().name)
    }

    @Test
    fun `decodes a compressed CNAME pointing back into the question`() {
        val question = encodeName("www.example.com") + byteArrayOf(0, 5, 0, 1) // CNAME, IN
        val pointerToQuestionStart = byteArrayOf(0xC0.toByte(), 0x0C) // pointer to offset 12
        val cnameRdata = pointerToQuestionStart
        val answer = pointerToQuestionStart +
            typeClassTtlRdata(5, cnameRdata)
        val message = buildResponse(
            id = 1,
            flags = FLAG_QR,
            question = question,
            answers = listOf(answer),
        )

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("www.example.com.", decoded.answers.single().name)
        assertEquals("www.example.com.", decoded.answers.single().rdata)
    }

    @Test
    fun `omits the EDNS OPT pseudo record from the additional section`() {
        val optRecord = byteArrayOf(0) + byteArrayOf(0, 41) + byteArrayOf(0x10, 0) +
            byteArrayOf(0, 0, 0, 0) + byteArrayOf(0, 0)
        val message = buildResponse(id = 1, flags = FLAG_QR, additional = listOf(optRecord))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertTrue(decoded.additional.isEmpty())
    }

    @Test
    fun `decodes an AAAA record`() {
        val ipv6 = byteArrayOf(
            0x20, 0x01, 0x0d, 0xb8.toByte(),
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 1,
        )
        val answer = encodeName("example.com") + typeClassTtlRdata(28, ipv6)
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("2001:db8:0:0:0:0:0:1", decoded.answers.single().rdata)
    }

    @Test
    fun `decodes an MX record`() {
        val rdata = byteArrayOf(0, 10) + encodeName("mail.example.com")
        val answer = encodeName("example.com") + typeClassTtlRdata(15, rdata)
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("10 mail.example.com.", decoded.answers.single().rdata)
    }

    @Test
    fun `decodes a TXT record with multiple segments`() {
        val segmentOne = "v=spf1 ".toByteArray(Charsets.US_ASCII)
        val segmentTwo = "-all".toByteArray(Charsets.US_ASCII)
        val rdata = byteArrayOf(segmentOne.size.toByte()) + segmentOne +
            byteArrayOf(segmentTwo.size.toByte()) + segmentTwo
        val answer = encodeName("example.com") + typeClassTtlRdata(16, rdata)
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("v=spf1 -all", decoded.answers.single().rdata)
    }

    @Test
    fun `decodes a SOA record`() {
        val rdata = encodeName("ns1.example.com") + encodeName("hostmaster.example.com") +
            byteArrayOf(0, 0, 0, 1) + byteArrayOf(0, 0, 0x0e, 0x10) +
            byteArrayOf(0, 0, 0x03, 0x84.toByte()) + byteArrayOf(0, 0x09, 0x3a, 0x80.toByte()) +
            byteArrayOf(0, 0, 0x0e, 0x10)
        val answer = encodeName("example.com") + typeClassTtlRdata(6, rdata)
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals(
            "ns1.example.com. hostmaster.example.com. 1 3600 900 604800 3600",
            decoded.answers.single().rdata,
        )
    }

    @Test
    fun `decodes a SRV record`() {
        val rdata = byteArrayOf(0, 1, 0, 5, 0x1f, 0x90.toByte()) + encodeName("target.example.com")
        val answer = encodeName("_svc._tcp.example.com") + typeClassTtlRdata(33, rdata)
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("1 5 8080 target.example.com.", decoded.answers.single().rdata)
    }

    @Test
    fun `decodes a CAA record`() {
        val tag = "issue"
        val value = "letsencrypt.org"
        val rdata = byteArrayOf(0, tag.length.toByte()) +
            tag.toByteArray(Charsets.US_ASCII) +
            value.toByteArray(Charsets.US_ASCII)
        val answer = encodeName("example.com") + typeClassTtlRdata(257, rdata)
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("0 issue letsencrypt.org", decoded.answers.single().rdata)
    }

    @Test
    fun `falls back to a hex dump for record types without a dedicated decoder`() {
        val rdata = byteArrayOf(0x01, 0x02, 0x0a, 0xff.toByte())
        val answer = encodeName("example.com") + typeClassTtlRdata(48, rdata) // DNSKEY
        val message = buildResponse(id = 1, flags = FLAG_QR, answers = listOf(answer))

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertEquals("01020aff", decoded.answers.single().rdata)
    }

    @Test
    fun `decodes an authoritative answer with authentic data and a non-zero response code`() {
        val message = buildResponse(id = 1, flags = FLAG_QR or FLAG_AA or FLAG_AD or RESPONSE_CODE_NXDOMAIN)

        val decoded = DnsMessageCodec.decodeResponse(message)

        assertTrue(decoded.authoritative)
        assertTrue(decoded.authenticData)
        assertEquals(RESPONSE_CODE_NXDOMAIN, decoded.responseCode)
    }

    @Test
    fun `encoding rejects a label longer than 63 bytes`() {
        val tooLong = "a".repeat(64)

        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.encodeQuery(
                id = 1,
                name = tooLong,
                type = DnsRecordType.A.code,
                recursionDesired = false,
            )
        }
    }

    @Test
    fun `encoding rejects an empty label from consecutive dots`() {
        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.encodeQuery(
                id = 1,
                name = "example..com",
                type = DnsRecordType.A.code,
                recursionDesired = false,
            )
        }
    }

    @Test
    fun `decoding a name without a terminating zero label throws`() {
        // A truncated question with a label that claims 5 bytes but only has 3 remaining.
        val message = buildResponse(id = 1, flags = FLAG_QR, question = byteArrayOf(5, 'a'.code.toByte(), 'b'.code.toByte()))

        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.decodeResponse(message)
        }
    }

    @Test
    fun `decoding a truncated compression pointer throws`() {
        val message = buildResponse(id = 1, flags = FLAG_QR, question = byteArrayOf(0xC0.toByte()))

        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.decodeResponse(message)
        }
    }

    @Test
    fun `decoding a self referencing compression pointer loop throws`() {
        // Header is 12 bytes; a question starting there that immediately points back to itself
        // never terminates and must be rejected rather than looping forever.
        val question = byteArrayOf(0xC0.toByte(), 0x0C)
        val message = buildResponse(id = 1, flags = FLAG_QR, question = question)

        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.decodeResponse(message)
        }
    }

    @Test
    fun `reading past the end of the message throws`() {
        // Header claims one question but the message ends immediately after the header.
        val out = java.io.ByteArrayOutputStream()
        val header = java.io.DataOutputStream(out)
        header.writeShort(1)
        header.writeShort(FLAG_QR)
        header.writeShort(1)
        header.writeShort(0)
        header.writeShort(0)
        header.writeShort(0)

        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.decodeResponse(out.toByteArray())
        }
    }

    private fun typeClassTtlRdata(type: Int, rdata: ByteArray): ByteArray {
        val header = byteArrayOf(
            (type shr 8).toByte(), type.toByte(),
            0, 1, // CLASS IN
            0, 0, 0, 60, // TTL
            (rdata.size shr 8).toByte(), rdata.size.toByte(),
        )
        return header + rdata
    }

    private fun encodeName(name: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        name.split('.').forEach { label ->
            out.write(label.length)
            out.write(label.toByteArray(Charsets.US_ASCII))
        }
        out.write(0)
        return out.toByteArray()
    }

    private fun buildResponse(
        id: Int,
        flags: Int,
        question: ByteArray = encodeName("example.com") + byteArrayOf(0, 1, 0, 1),
        answers: List<ByteArray> = emptyList(),
        authority: List<ByteArray> = emptyList(),
        additional: List<ByteArray> = emptyList(),
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val header = java.io.DataOutputStream(out)
        header.writeShort(id)
        header.writeShort(flags)
        header.writeShort(1)
        header.writeShort(answers.size)
        header.writeShort(authority.size)
        header.writeShort(additional.size)
        out.write(question)
        answers.forEach(out::write)
        authority.forEach(out::write)
        additional.forEach(out::write)
        return out.toByteArray()
    }

    private companion object {
        const val FLAG_QR = 0x8000
        const val FLAG_RA = 0x0080
        const val FLAG_AA = 0x0400
        const val FLAG_AD = 0x0020
        const val RESPONSE_CODE_NXDOMAIN = 3
    }
}
