package co.sorsby.debugtoolkit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

        composeRule.onNodeWithText("Before you start").assertDoesNotExist()
        composeRule.onAllNodesWithText("Overview").onFirst().assertIsDisplayed()
        assertEquals(AnalyticsConsent.GRANTED, awaitPersistedConsent())
    }

    @Test
    fun decliningConsentDeniesAndShowsMainContent() {
        composeRule.onNodeWithText("Decline").performClick()

        composeRule.onNodeWithText("Before you start").assertDoesNotExist()
        composeRule.onAllNodesWithText("Overview").onFirst().assertIsDisplayed()
        assertEquals(AnalyticsConsent.DENIED, awaitPersistedConsent())
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
}
