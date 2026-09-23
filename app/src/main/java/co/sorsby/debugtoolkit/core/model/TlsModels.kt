package co.sorsby.debugtoolkit.core.model

import java.time.Instant

data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: Instant,
    val validUntil: Instant,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val subjectAlternativeNames: List<String>,
)

data class TlsResult(
    val host: String,
    val port: Int,
    val protocol: String,
    val cipherSuite: String,
    val certificates: List<CertificateInfo>,
    val elapsedMs: Long,
)
