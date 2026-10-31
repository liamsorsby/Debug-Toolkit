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
    fun `ads do not initialize until both the setting and consent allow requests`() = runTest {
        val enabled = MutableStateFlow(false)
        val consentAllowsAds = MutableStateFlow(false)
        var starts = 0
        val job = AdsStartupController(enabled, consentAllowsAds) { starts++ }.start(this)

        advanceUntilIdle()
        assertEquals(0, starts)

        enabled.value = true
        advanceUntilIdle()
        assertEquals(0, starts)

        consentAllowsAds.value = true
        advanceUntilIdle()
        assertEquals(1, starts)

        enabled.value = false
        enabled.value = true
        consentAllowsAds.value = false
        consentAllowsAds.value = true
        advanceUntilIdle()
        assertEquals(1, starts)
        job.cancel()
    }

    @Test
    fun `enabled and consented on startup initializes once`() = runTest {
        val enabled = MutableStateFlow(true)
        val consentAllowsAds = MutableStateFlow(true)
        var starts = 0
        AdsStartupController(enabled, consentAllowsAds) { starts++ }.start(this)

        advanceUntilIdle()
        assertEquals(1, starts)
    }

    @Test
    fun `consent alone does not initialize ads when the app setting is disabled`() = runTest {
        val enabled = MutableStateFlow(false)
        val consentAllowsAds = MutableStateFlow(true)
        var starts = 0
        val job = AdsStartupController(enabled, consentAllowsAds) { starts++ }.start(this)

        advanceUntilIdle()
        assertEquals(0, starts)
        job.cancel()
    }
}
