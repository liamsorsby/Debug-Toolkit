package co.sorsby.debugtoolkit.data.tls

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SocketTlsInspectorTest {
    private lateinit var server: MockWebServer
    private lateinit var inspector: SocketTlsInspector

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
        server.start()
        inspector = SocketTlsInspector(clientCertificates.sslSocketFactory())
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `inspection returns negotiated connection and certificate chain`() = runTest {
        val result = inspector.inspect(server.url("/").toString())

        assertEquals("localhost", result.host)
        assertEquals(server.port, result.port)
        assertTrue(result.protocol.startsWith("TLS"))
        assertTrue(result.cipherSuite.isNotBlank())
        assertEquals(1, result.certificates.size)
        assertTrue(result.certificates.single().subject.contains("localhost"))
        assertTrue(result.certificates.single().subjectAlternativeNames.contains("localhost"))
    }
}
