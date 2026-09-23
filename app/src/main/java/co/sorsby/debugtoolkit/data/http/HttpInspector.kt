package co.sorsby.debugtoolkit.data.http

import co.sorsby.debugtoolkit.core.model.HeaderValue
import co.sorsby.debugtoolkit.core.model.HttpInspection
import co.sorsby.debugtoolkit.core.model.RedirectStep
import co.sorsby.debugtoolkit.core.network.await
import co.sorsby.debugtoolkit.domain.InputValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import kotlin.time.TimeSource

interface HttpInspector {
    suspend fun inspect(input: String, method: HttpMethod): HttpInspection
}

class OkHttpInspector(private val client: OkHttpClient) : HttpInspector {
    override suspend fun inspect(input: String, method: HttpMethod): HttpInspection =
        withContext(Dispatchers.IO) {
            val url = InputValidation.httpsUrl(input)
            val request = Request.Builder().url(url).method(method.name, null).build()
            val mark = TimeSource.Monotonic.markNow()
            client.newCall(request).await().use { response ->
                val contentType = response.body.contentType()
                val textual = contentType?.type == "text" ||
                    contentType?.subtype?.contains("json", ignoreCase = true) == true ||
                    contentType?.subtype?.contains("xml", ignoreCase = true) == true
                val previewBytes = if (method == HttpMethod.GET && textual) {
                    val source = response.body.source()
                    val buffer = Buffer()
                    while (buffer.size <= MAX_PREVIEW_BYTES) {
                        val remaining = MAX_PREVIEW_BYTES + 1L - buffer.size
                        val read = source.read(buffer, minOf(8_192L, remaining))
                        if (read == -1L) break
                    }
                    buffer.readByteArray()
                } else {
                    null
                }
                val truncated = previewBytes != null && previewBytes.size > MAX_PREVIEW_BYTES
                val safeBytes = previewBytes?.copyOf(minOf(previewBytes.size, MAX_PREVIEW_BYTES))
                HttpInspection(
                    status = response.code,
                    message = response.message,
                    protocol = response.protocol.toString(),
                    finalUrl = response.request.url.toString(),
                    elapsedMs = mark.elapsedNow().inWholeMilliseconds,
                    headers = response.headers.map { HeaderValue(it.first, it.second) },
                    redirects = response.redirects(),
                    bodyPreview = safeBytes?.toString(contentType?.charset() ?: Charsets.UTF_8),
                    bodyTruncated = truncated,
                    contentType = contentType?.toString(),
                )
            }
        }

    private fun Response.redirects(): List<RedirectStep> {
        val responses = generateSequence(priorResponse) { it.priorResponse }.toList().asReversed()
        return responses.mapNotNull { previous ->
            val location = previous.header("Location") ?: return@mapNotNull null
            RedirectStep(previous.code, previous.request.url.toString(), location)
        }
    }

    private companion object {
        const val MAX_PREVIEW_BYTES = 1024 * 1024
    }
}
