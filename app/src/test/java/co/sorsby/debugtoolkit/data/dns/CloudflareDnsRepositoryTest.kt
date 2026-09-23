package co.sorsby.debugtoolkit.data.dns

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CloudflareDnsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: CloudflareDnsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        repository = CloudflareDnsRepository(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true },
            server.url("/dns-query"),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `query maps answers and metadata`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "Status": 0,
                      "AD": true,
                      "RA": true,
                      "Answer": [{"name":"example.com.","type":1,"TTL":300,"data":"192.0.2.1"}],
                      "Authority": [{"name":"example.com.","type":2,"TTL":600,"data":"ns.example.com."}],
                      "Additional": [{"name":"ns.example.com.","type":1,"TTL":600,"data":"192.0.2.53"}]
                    }
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/dns-json"),
        )

        val result = repository.query("Example.COM", DnsRecordType.A)

        assertEquals(0, result.status)
        assertEquals(true, result.authenticatedData)
        assertEquals("192.0.2.1", result.records.single().value)
        assertEquals("ns.example.com.", result.authority.single().value)
        assertEquals("192.0.2.53", result.additional.single().value)
        val request = server.takeRequest()
        assertEquals("example.com", request.requestUrl?.queryParameter("name"))
        assertEquals("1", request.requestUrl?.queryParameter("type"))
        assertEquals("application/dns-json", request.headers["Accept"])
    }

    @Test
    fun `query converts PTR input`() = runTest {
        server.enqueue(MockResponse().setBody("""{"Status":3}"""))
        repository.query("192.0.2.1", DnsRecordType.PTR)
        assertEquals(
            "1.2.0.192.in-addr.arpa",
            server.takeRequest().requestUrl?.queryParameter("name"),
        )
    }

    @Test
    fun `query rejects HTTP failures`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        assertThrows(IllegalStateException::class.java) {
            runTest { repository.query("example.com", DnsRecordType.A) }
        }
    }
}
