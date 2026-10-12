package co.sorsby.debugtoolkit.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftUpdatePolicyTest {
    private val now = 10_000_000_000L

    private fun prompt(
        updateAvailable: Boolean = true,
        flexibleAllowed: Boolean = true,
        stalenessDays: Int? = SoftUpdatePolicy.MIN_STALENESS_DAYS,
        lastPromptAtMs: Long = 0L,
    ) = SoftUpdatePolicy.shouldPrompt(
        updateAvailable = updateAvailable,
        flexibleAllowed = flexibleAllowed,
        stalenessDays = stalenessDays,
        nowMs = now,
        lastPromptAtMs = lastPromptAtMs,
    )

    @Test
    fun promptsForStaleFlexibleUpdateNeverPromptedBefore() {
        assertTrue(prompt())
    }

    @Test
    fun doesNotPromptWhenNoUpdateOrFlexibleNotAllowed() {
        assertFalse(prompt(updateAvailable = false))
        assertFalse(prompt(flexibleAllowed = false))
    }

    @Test
    fun doesNotPromptForFreshOrUnknownStaleness() {
        assertFalse(prompt(stalenessDays = null))
        assertFalse(prompt(stalenessDays = SoftUpdatePolicy.MIN_STALENESS_DAYS - 1))
    }

    @Test
    fun throttlesRepeatPromptsWithinInterval() {
        assertFalse(prompt(lastPromptAtMs = now - SoftUpdatePolicy.PROMPT_INTERVAL_MS + 1))
        assertTrue(prompt(lastPromptAtMs = now - SoftUpdatePolicy.PROMPT_INTERVAL_MS))
    }

    @Test
    fun promptsWhenClockMovedBackwards() {
        assertTrue(prompt(lastPromptAtMs = now + 1_000L))
    }
}
