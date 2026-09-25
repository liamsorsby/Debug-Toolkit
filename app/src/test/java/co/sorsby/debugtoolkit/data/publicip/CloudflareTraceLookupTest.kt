package co.sorsby.debugtoolkit.data.publicip

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudflareTraceLookupTest {
    private lateinit var server: MockWebServer
    private lateinit var lookup: CloudflareTraceLookup

    @Before
    fun setUp() {
        server = MockWebServer()
        lookup = CloudflareTraceLookup(OkHttpClient(), server.url("/cdn-cgi/trace"))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `parses the trace response into a result`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("fl=1f1\nip=203.0.113.10\nloc=GB\ncolo=LHR\nwarp=on\n"),
        )

        val result = lookup.lookup()

        assertEquals("203.0.113.10", result.ipAddress)
        assertEquals("GB", result.countryCode)
        assertEquals("LHR", result.cloudflareColo)
        assertTrue(result.warpEnabled)
    }

    @Test
    fun `treats a missing warp field as disabled`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("ip=203.0.113.10\nloc=GB\ncolo=LHR\n"),
        )

        val result = lookup.lookup()

        assertFalse(result.warpEnabled)
    }

    @Test
    fun `fails when the service returns an error status`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { lookup.lookup() }
        }
    }

    @Test
    fun `fails when the response has no IP address`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("loc=GB\n"))

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { lookup.lookup() }
        }
    }
}
