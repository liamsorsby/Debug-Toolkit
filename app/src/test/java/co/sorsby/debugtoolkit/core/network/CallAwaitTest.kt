package co.sorsby.debugtoolkit.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAwaitTest {
    @Test
    fun `await propagates connection failures`() = runTest {
        val request = Request.Builder().url("http://127.0.0.1:1/").build()

        val error = runCatching {
            OkHttpClient().newCall(request).await()
        }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
    }
}
