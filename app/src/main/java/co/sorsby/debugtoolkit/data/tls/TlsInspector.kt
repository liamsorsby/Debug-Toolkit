package co.sorsby.debugtoolkit.data.tls

import co.sorsby.debugtoolkit.core.model.CertificateInfo
import co.sorsby.debugtoolkit.core.model.TlsResult
import co.sorsby.debugtoolkit.domain.InputValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.time.TimeSource

interface TlsInspector {
    suspend fun inspect(input: String): TlsResult
}

class SocketTlsInspector(
    private val socketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory,
) : TlsInspector {
    override suspend fun inspect(input: String): TlsResult = withContext(Dispatchers.IO) {
        val uri = URI(InputValidation.httpsUrl(input))
        val host = requireNotNull(uri.host)
        val port = if (uri.port == -1) 443 else uri.port
        val mark = TimeSource.Monotonic.markNow()
        (socketFactory.createSocket(host, port) as SSLSocket).use { socket ->
            socket.soTimeout = 10_000
            val parameters = socket.sslParameters
            parameters.endpointIdentificationAlgorithm = "HTTPS"
            socket.sslParameters = parameters
            socket.startHandshake()
            val session = socket.session
            TlsResult(
                host = host,
                port = port,
                protocol = session.protocol,
                cipherSuite = session.cipherSuite,
                certificates = session.peerCertificates
                    .filterIsInstance<X509Certificate>()
                    .map { it.toModel() },
                elapsedMs = mark.elapsedNow().inWholeMilliseconds,
            )
        }
    }

    private fun X509Certificate.toModel() = CertificateInfo(
        subject = subjectX500Principal.name,
        issuer = issuerX500Principal.name,
        serialNumber = serialNumber.toString(16).uppercase(),
        validFrom = notBefore.toInstant(),
        validUntil = notAfter.toInstant(),
        signatureAlgorithm = sigAlgName,
        publicKeyAlgorithm = publicKey.algorithm,
        subjectAlternativeNames = subjectAlternativeNames.orEmpty().mapNotNull { entry ->
            entry.getOrNull(1)?.toString()
        },
    )
}
