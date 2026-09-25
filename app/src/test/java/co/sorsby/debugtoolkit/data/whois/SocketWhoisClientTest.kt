package co.sorsby.debugtoolkit.data.whois

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SocketWhoisClientTest {

    @Test
    fun `follows a referral to the authoritative registry`() = runTest {
        val queries = mutableListOf<Pair<String, String>>()
        val transport = object : WhoisTransport {
            override suspend fun query(server: String, port: Int, queryText: String): String {
                queries += server to queryText
                return when (server) {
                    "whois.iana.org" -> "refer: whois.verisign-grs.com\ndomain: COM\n"
                    "whois.verisign-grs.com" -> "domain: EXAMPLE.COM\nstatus: ACTIVE\n"
                    else -> error("Unexpected server $server")
                }
            }
        }
        val client = SocketWhoisClient(transport)

        val result = client.lookup("example.com")

        assertEquals(
            listOf("whois.iana.org" to "example.com", "whois.verisign-grs.com" to "example.com"),
            queries,
        )
        assertEquals("whois.verisign-grs.com", result.server)
        assertEquals("domain: EXAMPLE.COM\nstatus: ACTIVE\n", result.rawText)
    }

    @Test
    fun `returns the IANA response as-is when there is no referral`() = runTest {
        val transport = object : WhoisTransport {
            override suspend fun query(server: String, port: Int, queryText: String): String =
                "domain: EXAMPLE\nstatus: RESERVED\n"
        }
        val client = SocketWhoisClient(transport)

        val result = client.lookup("example")

        assertEquals("whois.iana.org", result.server)
        assertEquals("domain: EXAMPLE\nstatus: RESERVED\n", result.rawText)
    }

    @Test
    fun `rejects an invalid domain without querying`() = runTest {
        val transport = object : WhoisTransport {
            override suspend fun query(server: String, port: Int, queryText: String): String =
                throw AssertionError("Should not query with an invalid domain.")
        }
        val client = SocketWhoisClient(transport)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { client.lookup(" ") }
        }
    }
}
