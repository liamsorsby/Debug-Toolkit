package co.sorsby.debugtoolkit.data.dns

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Encodes and decodes raw DNS messages on the wire (RFC 1035), for talking directly to a
 * nameserver over UDP/TCP rather than through a DNS-over-HTTPS resolver such as Cloudflare.
 *
 * This only needs to support what a debug tool actually shows the user: the standard header
 * flags, and readable rdata for the common record types. Record types this app supports but that
 * are rarely queried directly against a nameserver (for example DNSSEC signature records) fall
 * back to a hex dump of their raw rdata rather than a fully modelled parser for every type.
 */
object DnsMessageCodec {
    private const val DNS_CLASS_IN = 1
    private const val OPT_RECORD_TYPE = 41
    private const val EDNS_UDP_PAYLOAD_SIZE = 4096
    private const val FLAG_RECURSION_DESIRED = 0x0100
    private const val FLAG_AUTHORITATIVE_ANSWER = 0x0400
    private const val FLAG_TRUNCATED = 0x0200
    private const val FLAG_RECURSION_AVAILABLE = 0x0080
    private const val FLAG_AUTHENTIC_DATA = 0x0020
    private const val RESPONSE_CODE_MASK = 0x000F

    /**
     * Builds a single-question DNS query, including an EDNS0 OPT pseudo-record advertising a
     * larger UDP payload size so most answers do not need to fall back to TCP.
     */
    fun encodeQuery(id: Int, name: String, type: Int, recursionDesired: Boolean): ByteArray {
        val buffer = ByteArrayOutputStream()
        val out = DataOutputStream(buffer)
        out.writeShort(id)
        out.writeShort(if (recursionDesired) FLAG_RECURSION_DESIRED else 0)
        out.writeShort(1) // QDCOUNT
        out.writeShort(0) // ANCOUNT
        out.writeShort(0) // NSCOUNT
        out.writeShort(1) // ARCOUNT: the EDNS0 OPT record below
        writeName(out, name)
        out.writeShort(type)
        out.writeShort(DNS_CLASS_IN)
        writeOptRecord(out)
        return buffer.toByteArray()
    }

    private fun writeOptRecord(out: DataOutputStream) {
        out.writeByte(0) // root name
        out.writeShort(OPT_RECORD_TYPE)
        out.writeShort(EDNS_UDP_PAYLOAD_SIZE) // "class" field carries the UDP payload size
        out.writeInt(0) // extended RCODE, EDNS version, and flags
        out.writeShort(0) // RDLENGTH
    }

    private fun writeName(out: DataOutputStream, name: String) {
        val trimmed = name.trimEnd('.')
        if (trimmed.isEmpty()) {
            out.writeByte(0)
            return
        }
        trimmed.split('.').forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            require(bytes.size in 1..63) { "DNS label length out of range." }
            out.writeByte(bytes.size)
            out.write(bytes)
        }
        out.writeByte(0)
    }

    fun decodeResponse(bytes: ByteArray): DnsWireMessage {
        val reader = DnsByteReader(bytes)
        val id = reader.readUShort()
        val flags = reader.readUShort()
        val questionCount = reader.readUShort()
        val answerCount = reader.readUShort()
        val authorityCount = reader.readUShort()
        val additionalCount = reader.readUShort()
        repeat(questionCount) {
            reader.readName()
            reader.skip(4) // QTYPE + QCLASS
        }
        val answers = List(answerCount) { reader.readRecord() }
        val authority = List(authorityCount) { reader.readRecord() }
        val additional = List(additionalCount) { reader.readRecord() }
            .filterNot { it.type == OPT_RECORD_TYPE }
        return DnsWireMessage(
            id = id,
            authoritative = flags and FLAG_AUTHORITATIVE_ANSWER != 0,
            truncated = flags and FLAG_TRUNCATED != 0,
            recursionAvailable = flags and FLAG_RECURSION_AVAILABLE != 0,
            authenticData = flags and FLAG_AUTHENTIC_DATA != 0,
            responseCode = flags and RESPONSE_CODE_MASK,
            answers = answers,
            authority = authority,
            additional = additional,
        )
    }
}

data class DnsWireRecord(
    val name: String,
    val type: Int,
    val ttl: Long,
    val rdata: String,
)

data class DnsWireMessage(
    val id: Int,
    val authoritative: Boolean,
    val truncated: Boolean,
    val recursionAvailable: Boolean,
    val authenticData: Boolean,
    val responseCode: Int,
    val answers: List<DnsWireRecord>,
    val authority: List<DnsWireRecord>,
    val additional: List<DnsWireRecord>,
)

/** Sequential reader over a raw DNS message, supporting RFC 1035 name compression pointers. */
private class DnsByteReader(private val data: ByteArray) {
    var position = 0
        private set

    fun skip(count: Int) {
        position += count
    }

    fun readUByte(): Int {
        require(position < data.size) { "DNS message ended unexpectedly." }
        return (data[position++].toInt() and 0xFF)
    }

    fun readUShort(): Int = (readUByte() shl 8) or readUByte()

    fun readUInt(): Long = (readUShort().toLong() shl 16) or readUShort().toLong()

    fun readName(): String {
        val labels = mutableListOf<String>()
        var cursor = position
        var followedPointer = false
        var hops = 0
        while (true) {
            require(cursor < data.size) { "DNS message truncated while reading a name." }
            val length = data[cursor].toInt() and 0xFF
            when {
                length == 0 -> {
                    cursor += 1
                    if (!followedPointer) position = cursor
                    break
                }
                (length and 0xC0) == 0xC0 -> {
                    require(cursor + 1 < data.size) { "DNS message truncated while reading a name pointer." }
                    val pointer = ((length and 0x3F) shl 8) or (data[cursor + 1].toInt() and 0xFF)
                    if (!followedPointer) position = cursor + 2
                    followedPointer = true
                    cursor = pointer
                }
                else -> {
                    val start = cursor + 1
                    val end = start + length
                    require(end <= data.size) { "DNS message truncated while reading a label." }
                    labels += String(data, start, length, Charsets.US_ASCII)
                    cursor = end
                }
            }
            hops += 1
            require(hops < 128) { "DNS name compression loop detected." }
        }
        return if (labels.isEmpty()) "." else labels.joinToString(".") + "."
    }

    fun readRecord(): DnsWireRecord {
        val name = readName()
        val type = readUShort()
        readUShort() // record class; not surfaced to callers
        val ttl = readUInt()
        val rdataLength = readUShort()
        val rdataStart = position
        val rdata = decodeRdata(type, rdataStart, rdataLength)
        position = rdataStart + rdataLength
        return DnsWireRecord(name, type, ttl, rdata)
    }

    private fun decodeRdata(type: Int, start: Int, length: Int): String {
        val end = start + length
        return when (type) {
            1 -> decodeIpv4(start) // A
            28 -> decodeIpv6(start) // AAAA
            2, 5, 12, 39 -> { // NS, CNAME, PTR, DNAME
                position = start
                readName()
            }
            15 -> { // MX
                position = start
                val preference = readUShort()
                "$preference ${readName()}"
            }
            16 -> decodeTxt(start, end) // TXT
            6 -> decodeSoa(start) // SOA
            33 -> { // SRV
                position = start
                val priority = readUShort()
                val weight = readUShort()
                val port = readUShort()
                "$priority $weight $port ${readName()}"
            }
            257 -> decodeCaa(start, end) // CAA
            else -> (start until end).joinToString("") { "%02x".format(data[it]) }
        }
    }

    private fun decodeIpv4(start: Int): String =
        (start until start + 4).joinToString(".") { (data[it].toInt() and 0xFF).toString() }

    private fun decodeIpv6(start: Int): String =
        (0 until 8).joinToString(":") { group ->
            val high = data[start + group * 2].toInt() and 0xFF
            val low = data[start + group * 2 + 1].toInt() and 0xFF
            ((high shl 8) or low).toString(16)
        }

    private fun decodeTxt(start: Int, end: Int): String {
        val builder = StringBuilder()
        var cursor = start
        while (cursor < end) {
            val segmentLength = data[cursor].toInt() and 0xFF
            cursor += 1
            builder.append(String(data, cursor, segmentLength, Charsets.US_ASCII))
            cursor += segmentLength
        }
        return builder.toString()
    }

    private fun decodeSoa(start: Int): String {
        position = start
        val primaryNameserver = readName()
        val responsibleMailbox = readName()
        val serial = readUInt()
        val refresh = readUInt()
        val retry = readUInt()
        val expire = readUInt()
        val minimumTtl = readUInt()
        return "$primaryNameserver $responsibleMailbox $serial $refresh $retry $expire $minimumTtl"
    }

    private fun decodeCaa(start: Int, end: Int): String {
        val flag = data[start].toInt() and 0xFF
        val tagLength = data[start + 1].toInt() and 0xFF
        val tag = String(data, start + 2, tagLength, Charsets.US_ASCII)
        val valueStart = start + 2 + tagLength
        val value = String(data, valueStart, end - valueStart, Charsets.US_ASCII)
        return "$flag $tag $value"
    }
}
