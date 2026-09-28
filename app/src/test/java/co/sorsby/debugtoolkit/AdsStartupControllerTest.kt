package co.sorsby.debugtoolkit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdsStartupControllerTest {
    @Test
    fun `disabled on startup does not initialize until enabled`() = runTest {
        val enabled = MutableStateFlow(false)
        var starts = 0
        val job = AdsStartupController(enabled) { starts++ }.start(this)

        advanceUntilIdle()
        assertEquals(0, starts)

        enabled.value = true
        advanceUntilIdle()
        assertEquals(1, starts)

        enabled.value = false
        enabled.value = true
        advanceUntilIdle()
        assertEquals(1, starts)
        job.cancel()
    }

    @Test
    fun `enabled on startup initializes once`() = runTest {
        val enabled = MutableStateFlow(true)
        var starts = 0
        AdsStartupController(enabled) { starts++ }.start(this)

        advanceUntilIdle()
        assertEquals(1, starts)
    }
}
