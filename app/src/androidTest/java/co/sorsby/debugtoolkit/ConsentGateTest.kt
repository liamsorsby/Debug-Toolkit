package co.sorsby.debugtoolkit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Verifies the mandatory first-launch consent prompt: it appears whenever consent has never been
 * set, blocks access to the main app until a choice is made, and always resolves to either
 * granted or denied, never back to undecided.
 */
@RunWith(AndroidJUnit4::class)
class ConsentGateTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(UnsetConsentRule())
        .around(composeRule)
        .around(AccessibilityCheckRule())

    @Test
    fun consentGateBlocksMainContentUntilAnswered() {
        composeRule.onNodeWithText("Before you start").assertIsDisplayed()
        composeRule.onNodeWithText("Overview").assertDoesNotExist()
    }

    @Test
    fun acceptingConsentGrantsAndShowsMainContent() {
        composeRule.onNodeWithText("Accept").performClick()

        awaitConsentGateDismissed()
        composeRule.onAllNodesWithText("Overview").onFirst().assertIsDisplayed()
        assertEquals(AnalyticsConsent.GRANTED, awaitPersistedConsent())
    }

    @Test
    fun decliningConsentDeniesAndShowsMainContent() {
        composeRule.onNodeWithText("Decline").performClick()

        awaitConsentGateDismissed()
        composeRule.onAllNodesWithText("Overview").onFirst().assertIsDisplayed()
        assertEquals(AnalyticsConsent.DENIED, awaitPersistedConsent())
    }

    /**
     * Waits for the consent gate to leave the composition after a choice has been made.
     *
     * Answering the gate persists the choice through DataStore on a background dispatcher, and the
     * gate only disappears once that write is read back by the settings flow. Compose's automatic
     * synchronisation does not cover that round trip, so asserting the gate's absence straight
     * after the click races the write instead of waiting for it.
     */
    private fun awaitConsentGateDismissed() {
        composeRule.waitUntil(CONSENT_PERSIST_TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Before you start").fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * Reads the persisted consent value once it settles. Deliberately collects on [Dispatchers.IO]
     * rather than the calling thread: the click above dispatches [SettingsViewModel.setConsent]
     * onto the main dispatcher, so blocking the main thread here to wait for that write to land
     * would deadlock.
     */
    private fun awaitPersistedConsent(): AnalyticsConsent = runBlocking(Dispatchers.IO) {
        val repository = GlobalContext.get().get<SettingsRepository>()
        repository.settings.first { it.analyticsConsent != AnalyticsConsent.UNSET }.analyticsConsent
    }

    private companion object {
        /**
         * Upper bound for a consent choice to be persisted and read back. Generous enough to
         * absorb a heavily loaded CI emulator without leaving a genuinely stuck write to hang
         * until the whole test run is cancelled.
         */
        const val CONSENT_PERSIST_TIMEOUT_MS = 10_000L
    }
}
