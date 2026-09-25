package co.sorsby.debugtoolkit.data.dns

import co.sorsby.debugtoolkit.core.model.DnsRecord
import co.sorsby.debugtoolkit.core.model.DnsResult
import co.sorsby.debugtoolkit.core.network.await
import co.sorsby.debugtoolkit.domain.InputValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.TimeSource

interface DnsRepository {
    suspend fun query(input: String, type: DnsRecordType, nameserver: String? = null): DnsResult
}

class CloudflareDnsRepository(
    private val client: OkHttpClient,
    private val json: Json,
    private val endpoint: HttpUrl,
    private val nameserverResolver: DirectNameserverResolver,
) : DnsRepository {
    override suspend fun query(input: String, type: DnsRecordType, nameserver: String?): DnsResult {
        if (!nameserver.isNullOrBlank()) {
            return nameserverResolver.query(nameserver, input, type)
        }
        return withContext(Dispatchers.IO) {
            val name = if (type == DnsRecordType.PTR) {
                InputValidation.ptrName(input)
            } else {
                InputValidation.domain(input)
            }
            val url = endpoint.newBuilder()
                .addQueryParameter("name", name)
                .addQueryParameter("type", type.code.toString())
                .addQueryParameter("do", "1")
                .addQueryParameter("cd", "0")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()
            val mark = TimeSource.Monotonic.markNow()
            client.newCall(request).await().use { response ->
                check(response.isSuccessful) { "DNS service returned HTTP ${response.code}." }
                val body = response.body.string()
                val payload = json.decodeFromString<DnsPayload>(body)
                DnsResult(
                    status = payload.status,
                    authenticatedData = payload.authenticatedData,
                    recursionAvailable = payload.recursionAvailable,
                    records = payload.answers.orEmpty().map(DnsAnswer::toModel),
                    authority = payload.authority.orEmpty().map(DnsAnswer::toModel),
                    additional = payload.additional.orEmpty().map(DnsAnswer::toModel),
                    elapsedMs = mark.elapsedNow().inWholeMilliseconds,
                )
            }
        }
    }
}

@Serializable
private data class DnsPayload(
    @SerialName("Status") val status: Int,
    @SerialName("AD") val authenticatedData: Boolean = false,
    @SerialName("RA") val recursionAvailable: Boolean = false,
    @SerialName("Answer") val answers: List<DnsAnswer>? = null,
    @SerialName("Authority") val authority: List<DnsAnswer>? = null,
    @SerialName("Additional") val additional: List<DnsAnswer>? = null,
)

@Serializable
private data class DnsAnswer(
    val name: String,
    val type: Int,
    @SerialName("TTL") val ttl: Long,
    val data: String,
) {
    fun toModel() = DnsRecord(
        name = name,
        type = type,
        ttlSeconds = ttl,
        value = data,
    )
}
