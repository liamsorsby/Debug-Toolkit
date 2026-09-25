package co.sorsby.debugtoolkit.data.dns

import co.sorsby.debugtoolkit.core.model.DnsRecord
import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.domain.InputValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random
import kotlin.time.TimeSource

/**
 * Queries a specific nameserver directly over raw DNS, with recursion explicitly disabled. This is
 * what lets the DNS screen show the exact, non-recursive answer a given authoritative or
 * intermediate server would give, rather than the fully-resolved answer a recursive resolver like
 * Cloudflare would return.
 */
interface DirectNameserverResolver {
    suspend fun query(nameserver: String, input: String, type: DnsRecordType): DnsResult
}

/**
 * Default [DirectNameserverResolver] backed by real UDP sockets, falling back to TCP when a UDP
 * answer comes back truncated.
 */
class RawDnsResolver(private val port: Int = DEFAULT_DNS_PORT) : DirectNameserverResolver {

    override suspend fun query(nameserver: String, input: String, type: DnsRecordType): DnsResult =
        withContext(Dispatchers.IO) {
            val name = if (type == DnsRecordType.PTR) {
                InputValidation.ptrName(input)
            } else {
                InputValidation.domain(input)
            }
            val address = resolveNameserver(nameserver)
            val queryId = Random.nextInt(0, MAX_QUERY_ID)
            val query = DnsMessageCodec.encodeQuery(
                id = queryId,
                name = name,
                type = type.code,
                recursionDesired = false,
            )
            val mark = TimeSource.Monotonic.markNow()
            var response = DnsMessageCodec.decodeResponse(sendUdp(address, query))
            if (response.truncated) {
                response = DnsMessageCodec.decodeResponse(sendTcp(address, query))
            }
            check(response.id == queryId) { "DNS response did not match the query sent." }
            DnsResult(
                status = response.responseCode,
                authenticatedData = response.authenticData,
                recursionAvailable = response.recursionAvailable,
                authoritative = response.authoritative,
                records = response.answers.map(DnsWireRecord::toModel),
                authority = response.authority.map(DnsWireRecord::toModel),
                additional = response.additional.map(DnsWireRecord::toModel),
                elapsedMs = mark.elapsedNow().inWholeMilliseconds,
            )
        }

    private fun resolveNameserver(nameserver: String): InetAddress {
        val validated = InputValidation.nameserver(nameserver)
        return InetAddress.getByName(validated)
    }

    private fun sendUdp(address: InetAddress, query: ByteArray): ByteArray {
        DatagramSocket().use { socket ->
            socket.soTimeout = SOCKET_TIMEOUT_MS
            socket.send(DatagramPacket(query, query.size, address, port))
            val buffer = ByteArray(MAX_UDP_RESPONSE_BYTES)
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)
            return packet.data.copyOf(packet.length)
        }
    }

    private fun sendTcp(address: InetAddress, query: ByteArray): ByteArray {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(address, port), SOCKET_TIMEOUT_MS)
            socket.soTimeout = SOCKET_TIMEOUT_MS
            val output = DataOutputStream(socket.getOutputStream())
            output.writeShort(query.size)
            output.write(query)
            output.flush()
            val input = DataInputStream(socket.getInputStream())
            val length = input.readUnsignedShort()
            val response = ByteArray(length)
            input.readFully(response)
            return response
        }
    }

    private companion object {
        const val DEFAULT_DNS_PORT = 53
        const val SOCKET_TIMEOUT_MS = 5_000
        const val MAX_UDP_RESPONSE_BYTES = 4_096
        const val MAX_QUERY_ID = 0xFFFF
    }
}

private fun DnsWireRecord.toModel() = DnsRecord(
    name = name,
    type = type,
    ttlSeconds = ttl,
    value = rdata,
)
