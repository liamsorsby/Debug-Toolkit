package co.sorsby.debugtoolkit.review

import co.sorsby.debugtoolkit.core.model.ToolError
import co.sorsby.debugtoolkit.telemetry.DiagnosticTool
import co.sorsby.debugtoolkit.telemetry.JourneyTracker
import co.sorsby.debugtoolkit.telemetry.ToolJourney
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val start = 1_000L * day

    private class MemoryStore : ReviewPromptStore {
        override var firstUseAtMs = 0L
        override var successfulRuns = 0
        override var lastRequestAtMs = 0L
    }

    @Test
    fun policyRequiresEnoughRunsAndTime() {
        val now = start + 3 * day
        assertTrue(ReviewPromptPolicy.isEligible(3, start, 0L, now))
        assertFalse(ReviewPromptPolicy.isEligible(2, start, 0L, now))
        assertFalse(ReviewPromptPolicy.isEligible(3, now - day, 0L, now))
        assertFalse(ReviewPromptPolicy.isEligible(3, 0L, 0L, now))
    }

    @Test
    fun policyThrottlesRepeatRequests() {
        val now = start + 500 * day
        val interval = ReviewPromptPolicy.REQUEST_INTERVAL_MS
        assertFalse(ReviewPromptPolicy.isEligible(3, start, now - interval + 1, now))
        assertTrue(ReviewPromptPolicy.isEligible(3, start, now - interval, now))
        assertTrue(ReviewPromptPolicy.isEligible(3, start, now + day, now))
    }

    @Test
    fun coordinatorRecordsFirstUseOnlyOnce() {
        val store = MemoryStore()
        ReviewPromptCoordinator(store) { start }
        ReviewPromptCoordinator(store) { start + day }
        assertEquals(start, store.firstUseAtMs)
    }

    @Test
    fun coordinatorRequestsOnNavigationAfterEligibleSuccess() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val store = MemoryStore().apply { firstUseAtMs = start; successfulRuns = 2 }
        val now = start + 3 * day
        val coordinator = ReviewPromptCoordinator(store) { now }
        var requests = 0
        val job = coordinator.requests.onEach { requests++ }.launchIn(this)

        coordinator.onNavigation()
        assertEquals(0, requests)

        coordinator.recordToolSuccess()
        assertEquals(0, requests)
        coordinator.onNavigation()
        assertEquals(1, requests)
        assertEquals(now, store.lastRequestAtMs)

        coordinator.recordToolSuccess()
        coordinator.onNavigation()
        assertEquals(1, requests)
        job.cancel()
    }

    @Test
    fun coordinatorBuffersRequestUntilCollected() = runTest {
        val store = MemoryStore().apply { firstUseAtMs = start; successfulRuns = 5 }
        val coordinator = ReviewPromptCoordinator(store) { start + 3 * day }
        coordinator.recordToolSuccess()
        coordinator.onNavigation()
        withTimeout(1_000) { coordinator.requests.first() }
    }

    @Test
    fun coordinatorCapsStoredRunCount() {
        val store = MemoryStore().apply { firstUseAtMs = start; successfulRuns = 1_000 }
        ReviewPromptCoordinator(store) { start }.recordToolSuccess()
        assertEquals(1_000, store.successfulRuns)
    }

    @Test
    fun trackerForwardsCallsAndCountsOnlySuccesses() {
        val store = MemoryStore()
        val delegate = RecordingTracker()
        val tracker = ReviewTrackingJourneyTracker(delegate, ReviewPromptCoordinator(store) { start })

        tracker.trackScreen("dns")
        tracker.trackNavigation("overview", "dns", "drawer")
        tracker.trackAction("open_drawer", "dns")
        tracker.startTool(DiagnosticTool.DNS).succeed()
        tracker.startTool(DiagnosticTool.DNS).fail(ToolError.NETWORK, IOException())
        tracker.startTool(DiagnosticTool.DNS).cancel()

        assertEquals(
            listOf(
                "screen:dns",
                "nav:overview>dns:drawer",
                "action:open_drawer@dns",
                "start:dns", "succeed",
                "start:dns", "fail:NETWORK",
                "start:dns", "cancel",
            ),
            delegate.calls,
        )
        assertEquals(1, store.successfulRuns)
    }

    private class RecordingTracker : JourneyTracker {
        val calls = mutableListOf<String>()

        override fun trackScreen(route: String) {
            calls += "screen:$route"
        }

        override fun trackNavigation(from: String?, to: String, source: String) {
            calls += "nav:$from>$to:$source"
        }

        override fun trackAction(action: String, screen: String?) {
            calls += "action:$action@$screen"
        }

        override fun startTool(tool: DiagnosticTool): ToolJourney {
            calls += "start:${tool.eventValue}"
            return object : ToolJourney {
                override fun succeed() {
                    calls += "succeed"
                }

                override fun fail(error: ToolError, cause: Exception) {
                    calls += "fail:${error.name}"
                }

                override fun cancel() {
                    calls += "cancel"
                }
            }
        }
    }
}
