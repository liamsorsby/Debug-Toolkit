package co.sorsby.debugtoolkit.data.speed

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CloudflareSpeedTestRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `test performs bounded samples and reports measurements`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val bytes = request.requestUrl?.queryParameter("bytes")?.toInt() ?: 0
                return if (request.method == "POST") {
                    MockResponse().setResponseCode(200)
                } else {
                    MockResponse()
                        .setBody("0".repeat(bytes))
                        .addHeader("Server-Timing", "cfSpeedEdge;dur=0.1")
                }
            }
        }
        val repository = CloudflareSpeedTestRepository(OkHttpClient(), server.url("/"))

        val result = repository.run()

        assertTrue(result.latencyMs >= 0.0)
        assertTrue(result.jitterMs >= 0.0)
        assertTrue(result.downloadMbps > 0.0)
        assertTrue(result.uploadMbps > 0.0)
        assertEquals(7_200_000L, result.transferredBytes)
        assertEquals(11, server.requestCount)
    }

    @Test
    fun `test rejects latency service failures`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val repository = CloudflareSpeedTestRepository(OkHttpClient(), server.url("/"))

        val error = runCatching { repository.run() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `test supports latency responses without server timing`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val bytes = request.requestUrl?.queryParameter("bytes")?.toInt() ?: 0
                return if (request.method == "POST") {
                    MockResponse()
                } else {
                    MockResponse().setBody("0".repeat(bytes))
                }
            }
        }
        val repository = CloudflareSpeedTestRepository(OkHttpClient(), server.url("/"))

        assertTrue(repository.run().latencyMs >= 0.0)
    }

    @Test
    fun `test reports download failures`() = runTest {
        val requests = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (requests.incrementAndGet() == 7) {
                    return MockResponse().setResponseCode(503)
                }
                return MockResponse()
                    .setBody("")
                    .addHeader("Server-Timing", "cfSpeedEdge;dur=0")
            }
        }
        val repository = CloudflareSpeedTestRepository(OkHttpClient(), server.url("/"))

        assertTrue(runCatching { repository.run() }.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `test reports upload failures`() = runTest {
        val requests = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val count = requests.incrementAndGet()
                if (count == 10) return MockResponse().setResponseCode(503)
                val bytes = request.requestUrl?.queryParameter("bytes")?.toInt() ?: 0
                return MockResponse()
                    .setBody(if (request.method == "GET") "0".repeat(bytes) else "")
                    .addHeader("Server-Timing", "cfSpeedEdge;dur=0")
            }
        }
        val repository = CloudflareSpeedTestRepository(OkHttpClient(), server.url("/"))

        assertTrue(runCatching { repository.run() }.exceptionOrNull() is IllegalStateException)
    }
}
