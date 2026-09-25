package co.sorsby.debugtoolkit.data.publicip

import co.sorsby.debugtoolkit.core.model.PublicIpResult
import co.sorsby.debugtoolkit.core.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.TimeSource

interface PublicIpLookup {
    suspend fun lookup(): PublicIpResult
}

/**
 * Looks up this device's public IP address and the Cloudflare edge location handling its
 * traffic, using Cloudflare's undocumented but widely relied upon `/cdn-cgi/trace` endpoint. No
 * API key or third party geolocation service is required.
 */
class CloudflareTraceLookup(
    private val client: OkHttpClient,
    private val endpoint: HttpUrl,
) : PublicIpLookup {
    override suspend fun lookup(): PublicIpResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(endpoint).build()
        val mark = TimeSource.Monotonic.markNow()
        client.newCall(request).await().use { response ->
            check(response.isSuccessful) { "Trace service returned HTTP ${response.code}." }
            val fields = CloudflareTraceParser.parse(response.body.string())
            PublicIpResult(
                ipAddress = checkNotNull(fields["ip"]) { "Trace response did not include an IP address." },
                countryCode = fields["loc"],
                cloudflareColo = fields["colo"],
                warpEnabled = fields["warp"] == "on" || fields["warp"] == "plus",
                elapsedMs = mark.elapsedNow().inWholeMilliseconds,
            )
        }
    }
}
