package co.sorsby.debugtoolkit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.sorsby.debugtoolkit.data.dns.DnsRepository
import co.sorsby.debugtoolkit.data.http.HttpInspector
import co.sorsby.debugtoolkit.data.network.NetworkMonitor
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import co.sorsby.debugtoolkit.data.speed.SpeedTestRepository
import co.sorsby.debugtoolkit.data.tls.TlsInspector
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.feature.DnsViewModel
import co.sorsby.debugtoolkit.feature.HttpViewModel
import co.sorsby.debugtoolkit.feature.NetworkViewModel
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.feature.SpeedViewModel
import co.sorsby.debugtoolkit.feature.TlsViewModel
import co.sorsby.debugtoolkit.telemetry.JourneyTracker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun applicationPackageIsCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("co.sorsby.debugtoolkit", context.packageName)
    }

    @Test
    fun productionDependencyGraphResolves() {
        val koin = GlobalContext.get()
        assertNotNull(koin.get<SettingsRepository>())
        assertNotNull(koin.get<NetworkMonitor>())
        assertNotNull(koin.get<SpeedTestRepository>())
        assertNotNull(koin.get<DnsRepository>())
        assertNotNull(koin.get<TlsInspector>())
        assertNotNull(koin.get<HttpInspector>())
        assertNotNull(koin.get<JourneyTracker>())
        assertNotNull(koin.get<SettingsViewModel>())
        assertNotNull(koin.get<NetworkViewModel>())
        assertNotNull(koin.get<SpeedViewModel>())
        assertNotNull(koin.get<DnsViewModel>())
        assertNotNull(koin.get<TlsViewModel>())
        assertNotNull(koin.get<HttpViewModel>())
    }

    @Test
    fun bottomNavigationOpensToolsCatalog() {
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onAllNodesWithText("Certificate inspector").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("DNS lookup").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("HTTP inspector").onFirst().assertIsDisplayed()
    }

    @Test
    fun networkTabShowsContinuousMonitor() {
        composeRule.onNodeWithText("Network").performClick()
        composeRule.onNodeWithText("MONITORING LIVE").assertIsDisplayed()
        composeRule.onNodeWithText("Connection details").assertIsDisplayed()
        composeRule.onNodeWithText("Signal and link properties refresh every two seconds")
            .assertIsDisplayed()
    }

    @Test
    fun drawerOpensSettings() {
        composeRule.onNodeWithContentDescription("Open navigation").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeRule.onNodeWithText("Optional analytics").assertIsDisplayed()
    }

    @Test
    fun drawerOpensEveryDiagnostic() {
        openDrawerDestination("Speed test")
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.speed_intro))
            .assertIsDisplayed()

        openDrawerDestination("Certificate inspector")
        composeRule.onNodeWithText("Inspect certificate").assertIsDisplayed()

        openDrawerDestination("DNS lookup")
        composeRule.onNodeWithText("Query DNS").assertIsDisplayed()

        openDrawerDestination("HTTP inspector")
        composeRule.onNodeWithText("Inspect response").assertIsDisplayed()
        composeRule.onNodeWithText("Show response body")
            .assertIsDisplayed()
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun settingsRepositoryPersistsSelections() = runBlocking {
        val repository = GlobalContext.get().get<SettingsRepository>()
        repository.setThemeMode(ThemeMode.DARK)
        repository.setAnalyticsConsent(AnalyticsConsent.DENIED)

        val settings = repository.settings.first {
            it.themeMode == ThemeMode.DARK &&
                it.analyticsConsent == AnalyticsConsent.DENIED
        }

        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(AnalyticsConsent.DENIED, settings.analyticsConsent)
        repository.setThemeMode(ThemeMode.SYSTEM)
        repository.setAnalyticsConsent(AnalyticsConsent.UNSET)
    }

    private fun openDrawerDestination(label: String) {
        composeRule.onNodeWithContentDescription("Open navigation").performClick()
        composeRule.onAllNodesWithText(label).onLast().performClick()
    }
}
