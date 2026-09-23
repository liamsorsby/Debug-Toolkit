package co.sorsby.debugtoolkit.data.http

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpInspectorTest {
    private lateinit var server: MockWebServer
    private lateinit var inspector: OkHttpInspector

    @Before
    fun setUp() {
        val certificate = HeldCertificate.Builder()
            .commonName("localhost")
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificate.certificate)
            .build()
        server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        inspector = OkHttpInspector(
            OkHttpClient.Builder()
                .sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager,
                )
                .build(),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `GET reports redirects duplicate headers and text body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/final"))
        server.enqueue(
            MockResponse()
                .setBody("response")
                .addHeader("Content-Type", "text/plain; charset=utf-8")
                .addHeader("X-Value", "one")
                .addHeader("X-Value", "two"),
        )

        val result = inspector.inspect(server.url("/start").toString(), HttpMethod.GET)

        assertEquals(200, result.status)
        assertEquals("response", result.bodyPreview)
        assertFalse(result.bodyTruncated)
        assertEquals(1, result.redirects.size)
        assertEquals(2, result.headers.count { it.name.equals("X-Value", ignoreCase = true) })
    }

    @Test
    fun `HEAD does not read a body`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("ignored")
                .addHeader("Content-Type", "text/plain"),
        )

        val result = inspector.inspect(server.url("/").toString(), HttpMethod.HEAD)

        assertNull(result.bodyPreview)
        assertFalse(result.bodyTruncated)
    }

    @Test
    fun `binary bodies are not rendered and text is capped`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("binary")
                .addHeader("Content-Type", "application/octet-stream"),
        )
        assertNull(
            inspector.inspect(server.url("/binary").toString(), HttpMethod.GET).bodyPreview,
        )

        server.enqueue(
            MockResponse()
                .setBody("x".repeat(1024 * 1024 + 10))
                .addHeader("Content-Type", "application/json"),
        )
        val capped = inspector.inspect(server.url("/large").toString(), HttpMethod.GET)
        assertTrue(capped.bodyTruncated)
        assertEquals(1024 * 1024, capped.bodyPreview?.length)
    }

    @Test
    fun `XML bodies are rendered and missing content types are not guessed`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("<result />")
                .addHeader("Content-Type", "application/xml"),
        )
        assertEquals(
            "<result />",
            inspector.inspect(server.url("/xml").toString(), HttpMethod.GET).bodyPreview,
        )

        server.enqueue(MockResponse().setBody("unknown"))
        assertNull(
            inspector.inspect(server.url("/unknown").toString(), HttpMethod.GET).bodyPreview,
        )
    }
}
