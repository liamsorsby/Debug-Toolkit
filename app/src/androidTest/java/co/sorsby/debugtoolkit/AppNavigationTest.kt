package co.sorsby.debugtoolkit

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
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
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    // Every test in this class exercises the main app content, so consent is granted before
    // MainActivity launches. The consent gate itself is covered separately in ConsentGateTest.
    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(GrantPermissionRule.grant(*requiredWifiTestPermissions()))
        .around(GrantedConsentRule())
        .around(composeRule)
        .around(AccessibilityCheckRule())

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
        composeRule.onNodeWithText("Connection details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Signal and link properties update live as they change")
            .performScrollTo()
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
    fun settingsHidesUndecidedConsentOption() {
        composeRule.onNodeWithContentDescription("Open navigation").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Not decided").assertDoesNotExist()
        composeRule.onNodeWithText("Granted").assertIsDisplayed()
        composeRule.onNodeWithText("Denied").assertIsDisplayed()
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
        repository.setAnalyticsConsent(AnalyticsConsent.GRANTED)
    }

    private fun openDrawerDestination(label: String) {
        composeRule.onNodeWithContentDescription("Open navigation").performClick()
        composeRule.onAllNodesWithText(label).onLast().performClick()
    }
}

/**
 * Mirrors [co.sorsby.debugtoolkit.ui.screens.NetworkScreen]'s permission requirements so the
 * network monitoring tab renders its granted-state UI during instrumentation tests, rather than
 * the permission request card.
 */
private fun requiredWifiTestPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
