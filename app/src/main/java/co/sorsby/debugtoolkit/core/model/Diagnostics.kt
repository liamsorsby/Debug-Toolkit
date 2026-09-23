package co.sorsby.debugtoolkit.core.model

import java.time.Instant

data class NetworkSnapshot(
    val sampledAtEpochMillis: Long = 0,
    val connected: Boolean = false,
    val validated: Boolean = false,
    val metered: Boolean = false,
    val transports: Set<NetworkTransport> = emptySet(),
    val localAddresses: List<String> = emptyList(),
    val wifiRssiDbm: Int? = null,
    val wifiSignal: WifiSignal = WifiSignal.UNAVAILABLE,
    val linkDownKbps: Int? = null,
    val linkUpKbps: Int? = null,
)

enum class NetworkTransport {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    BLUETOOTH,
}

enum class WifiSignal {
    EXCELLENT,
    GOOD,
    FAIR,
    WEAK,
    UNAVAILABLE,
}

data class SpeedResult(
    val latencyMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val transferredBytes: Long,
    val measuredAt: Instant,
)

data class DnsRecord(
    val name: String,
    val type: Int,
    val ttlSeconds: Long,
    val value: String,
)

data class DnsResult(
    val status: Int,
    val authenticatedData: Boolean,
    val recursionAvailable: Boolean,
    val records: List<DnsRecord>,
    val authority: List<DnsRecord>,
    val additional: List<DnsRecord>,
    val elapsedMs: Long,
)

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

data class HeaderValue(val name: String, val value: String)

data class RedirectStep(val status: Int, val from: String, val to: String)

data class HttpInspection(
    val status: Int,
    val message: String,
    val protocol: String,
    val finalUrl: String,
    val elapsedMs: Long,
    val headers: List<HeaderValue>,
    val redirects: List<RedirectStep>,
    val bodyPreview: String?,
    val bodyTruncated: Boolean,
    val contentType: String?,
)

sealed interface ToolState<out T> {
    data object Idle : ToolState<Nothing>
    data object Loading : ToolState<Nothing>
    data class Success<T>(val value: T) : ToolState<T>
    data class Error(val type: ToolError) : ToolState<Nothing>
}

enum class ToolError {
    INVALID_INPUT,
    NETWORK,
    SERVICE,
    UNKNOWN,
}
