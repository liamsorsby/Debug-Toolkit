package co.sorsby.debugtoolkit.data.dns

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket

/**
 * Exercises [RawDnsResolver] against real loopback UDP/TCP servers rather than mocks, so the wire
 * behaviour (recursion disabled, TCP fallback on truncation) is proven end-to-end.
 */
class RawDnsResolverTest {

    @Test
    fun `sends a non-recursive UDP query and decodes the answer`() = runTest {
        DatagramSocket(0).use { server ->
            val resolver = RawDnsResolver(port = server.localPort)
            val serverThread = Thread {
                val buffer = ByteArray(512)
                val packet = DatagramPacket(buffer, buffer.size)
                server.receive(packet)
                val queryId = readId(buffer)
                assertEquals(0, buffer[2].toInt() and 0x01) // RD bit must be clear
                val response = aRecordResponse(queryId, "192.0.2.1")
                server.send(DatagramPacket(response, response.size, packet.address, packet.port))
            }
            serverThread.start()

            val result = resolver.query("127.0.0.1", "example.com", DnsRecordType.A)

            serverThread.join(2_000)
            assertEquals("192.0.2.1", result.records.single().value)
            assertFalse(result.authoritative)
        }
    }

    @Test
    fun `falls back to TCP when the UDP answer is truncated`() = runTest {
        val port = ServerSocket(0).use { it.localPort }
        DatagramSocket(port).use { udpServer ->
            ServerSocket(port).use { tcpServer ->
                val resolver = RawDnsResolver(port = port)

                val udpThread = Thread {
                    val buffer = ByteArray(512)
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpServer.receive(packet)
                    val response = truncatedResponse(readId(buffer))
                    udpServer.send(
                        DatagramPacket(response, response.size, packet.address, packet.port),
                    )
                }
                udpThread.start()

                val tcpThread = Thread {
                    tcpServer.accept().use { socket ->
                        val input = DataInputStream(socket.getInputStream())
                        val length = input.readUnsignedShort()
                        val queryBytes = ByteArray(length)
                        input.readFully(queryBytes)
                        val response = aRecordResponse(readId(queryBytes), "192.0.2.9")
                        val output = DataOutputStream(socket.getOutputStream())
                        output.writeShort(response.size)
                        output.write(response)
                        output.flush()
                    }
                }
                tcpThread.start()

                val result = resolver.query("127.0.0.1", "example.com", DnsRecordType.A)

                udpThread.join(2_000)
                tcpThread.join(2_000)
                assertEquals("192.0.2.9", result.records.single().value)
            }
        }
    }

    private fun readId(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)

    private fun aRecordResponse(id: Int, address: String): ByteArray {
        val addressBytes = address.split('.').map { it.toInt().toByte() }.toByteArray()
        val answer = encodeName("example.com") +
            byteArrayOf(0, 1, 0, 1, 0, 0, 0, 60, 0, 4) +
            addressBytes
        return buildMessage(id = id, flags = FLAG_QR or FLAG_RA, answers = listOf(answer))
    }

    private fun truncatedResponse(id: Int): ByteArray =
        buildMessage(id = id, flags = FLAG_QR or FLAG_TC)

    private fun encodeName(name: String): ByteArray {
        val out = ByteArrayOutputStream()
        name.split('.').forEach { label ->
            out.write(label.length)
            out.write(label.toByteArray(Charsets.US_ASCII))
        }
        out.write(0)
        return out.toByteArray()
    }

    private fun buildMessage(
        id: Int,
        flags: Int,
        answers: List<ByteArray> = emptyList(),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val header = DataOutputStream(out)
        header.writeShort(id)
        header.writeShort(flags)
        header.writeShort(1) // QDCOUNT
        header.writeShort(answers.size)
        header.writeShort(0)
        header.writeShort(0)
        out.write(encodeName("example.com"))
        out.write(byteArrayOf(0, 1, 0, 1)) // QTYPE A, QCLASS IN
        answers.forEach(out::write)
        return out.toByteArray()
    }

    private companion object {
        const val FLAG_QR = 0x8000
        const val FLAG_RA = 0x0080
        const val FLAG_TC = 0x0200
    }
}
