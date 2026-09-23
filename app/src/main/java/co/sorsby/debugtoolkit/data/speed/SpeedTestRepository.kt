package co.sorsby.debugtoolkit.data.speed

import co.sorsby.debugtoolkit.core.model.SpeedResult
import co.sorsby.debugtoolkit.core.network.await
import co.sorsby.debugtoolkit.domain.SpeedMath
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Buffer
import java.time.Instant
import kotlin.coroutines.coroutineContext
import kotlin.time.TimeSource

interface SpeedTestRepository {
    suspend fun run(): SpeedResult
}

class CloudflareSpeedTestRepository(
    private val client: OkHttpClient,
    private val endpoint: HttpUrl,
) : SpeedTestRepository {
    override suspend fun run(): SpeedResult = withContext(Dispatchers.IO) {
        latencySample()
        val latencies = List(LATENCY_SAMPLES) {
            coroutineContext.ensureActive()
            latencySample()
        }
        val download = transferDownload(DOWNLOAD_SAMPLE_BYTES)
        val upload = transferUpload(UPLOAD_SAMPLE_BYTES)
        SpeedResult(
            latencyMs = latencies.average(),
            jitterMs = SpeedMath.jitterMs(latencies),
            downloadMbps = download.mbps,
            uploadMbps = upload.mbps,
            transferredBytes = download.bytes + upload.bytes,
            measuredAt = Instant.now(),
        )
    }

    private suspend fun latencySample(): Double {
        val request = Request.Builder().url(downloadUrl(0)).build()
        val mark = TimeSource.Monotonic.markNow()
        client.newCall(request).await().use { response ->
            check(response.isSuccessful) { "Speed service returned HTTP ${response.code}." }
            response.body.close()
            val serverMs = response.header("Server-Timing")
                ?.let(SERVER_DURATION::find)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()
                ?: 0.0
            return (mark.elapsedNow().inWholeNanoseconds / 1_000_000.0 - serverMs)
                .coerceAtLeast(0.0)
        }
    }

    private suspend fun transferDownload(sampleSizes: List<Long>): TransferMeasurement {
        var totalBytes = 0L
        var totalNanos = 0L
        for (size in sampleSizes) {
            coroutineContext.ensureActive()
            val request = Request.Builder().url(downloadUrl(size)).build()
            val mark = TimeSource.Monotonic.markNow()
            client.newCall(request).await().use { response ->
                check(response.isSuccessful) { "Download test returned HTTP ${response.code}." }
                val source = response.body.source()
                val buffer = Buffer()
                while (true) {
                    coroutineContext.ensureActive()
                    val read = source.read(buffer, BUFFER_SIZE)
                    if (read == -1L) break
                    totalBytes += read
                    buffer.clear()
                }
            }
            totalNanos += mark.elapsedNow().inWholeNanoseconds
        }
        return TransferMeasurement(totalBytes, SpeedMath.megabitsPerSecond(totalBytes, totalNanos))
    }

    private suspend fun transferUpload(sampleSizes: List<Long>): TransferMeasurement {
        var totalBytes = 0L
        var totalNanos = 0L
        for (size in sampleSizes) {
            coroutineContext.ensureActive()
            val request = Request.Builder()
                .url(
                    endpoint.newBuilder()
                        .addPathSegment("__up")
                        .addQueryParameter("bytes", size.toString())
                        .build(),
                )
                .post(ZeroRequestBody(size))
                .build()
            val mark = TimeSource.Monotonic.markNow()
            client.newCall(request).await().use { response ->
                check(response.isSuccessful) { "Upload test returned HTTP ${response.code}." }
                response.body.close()
            }
            totalNanos += mark.elapsedNow().inWholeNanoseconds
            totalBytes += size
        }
        return TransferMeasurement(totalBytes, SpeedMath.megabitsPerSecond(totalBytes, totalNanos))
    }

    private fun downloadUrl(bytes: Long) = endpoint.newBuilder()
        .addPathSegment("__down")
        .addQueryParameter("bytes", bytes.toString())
        .build()

    private data class TransferMeasurement(val bytes: Long, val mbps: Double)

    private class ZeroRequestBody(private val byteCount: Long) : RequestBody() {
        override fun contentType() = "application/octet-stream".toMediaType()
        override fun contentLength() = byteCount

        override fun writeTo(sink: BufferedSink) {
            val bytes = ByteArray(BUFFER_SIZE.toInt())
            var remaining = byteCount
            while (remaining > 0) {
                val count = minOf(remaining, bytes.size.toLong()).toInt()
                sink.write(bytes, 0, count)
                remaining -= count
            }
        }
    }

    private companion object {
        const val LATENCY_SAMPLES = 5
        const val BUFFER_SIZE = 8_192L
        val DOWNLOAD_SAMPLE_BYTES = listOf(100_000L, 1_000_000L, 5_000_000L)
        val UPLOAD_SAMPLE_BYTES = listOf(100_000L, 1_000_000L)
        val SERVER_DURATION = Regex("""cfSpeed(?:Edge|Worker);dur=([0-9.]+)""")
    }
}
