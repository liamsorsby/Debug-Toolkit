package co.sorsby.debugtoolkit.data.whois

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Sends a raw WHOIS query (RFC 3912) to a server on port 43 and returns the full plain text
 * response. WHOIS predates any structured wire format: a client opens a TCP connection, writes
 * the query followed by a CRLF, and reads whatever free text the server sends back until it
 * closes the connection.
 */
interface WhoisTransport {
    suspend fun query(server: String, port: Int, queryText: String): String
}

class SocketWhoisTransport : WhoisTransport {
    override suspend fun query(server: String, port: Int, queryText: String): String =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(server, port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = READ_TIMEOUT_MS
                socket.getOutputStream().apply {
                    write("$queryText\r\n".toByteArray(StandardCharsets.US_ASCII))
                    flush()
                }
                socket.getInputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val response = StringBuilder()
                    val chunk = CharArray(CHUNK_CHARS)
                    while (response.length < MAX_RESPONSE_CHARS) {
                        val read = reader.read(chunk, 0, minOf(chunk.size, MAX_RESPONSE_CHARS - response.length))
                        if (read == -1) break
                        response.append(chunk, 0, read)
                    }
                    response.toString()
                }
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 8_000
        // A misbehaving server could stream indefinitely; real WHOIS replies are a few KB.
        const val MAX_RESPONSE_CHARS = 256 * 1024
        const val CHUNK_CHARS = 4_096
    }
}
