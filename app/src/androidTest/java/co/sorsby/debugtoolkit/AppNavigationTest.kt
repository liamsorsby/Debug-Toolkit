package co.sorsby.debugtoolkit

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import co.sorsby.debugtoolkit.data.dns.DnsRepository
import co.sorsby.debugtoolkit.data.http.HttpInspector
import co.sorsby.debugtoolkit.data.lan.LanScanner
import co.sorsby.debugtoolkit.data.network.NetworkMonitor
import co.sorsby.debugtoolkit.data.ping.PingRunner
import co.sorsby.debugtoolkit.data.ping.TracerouteRunner
import co.sorsby.debugtoolkit.data.portscan.PortScanner
import co.sorsby.debugtoolkit.data.publicip.PublicIpLookup
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import co.sorsby.debugtoolkit.data.speed.SpeedTestRepository
import co.sorsby.debugtoolkit.data.tls.TlsInspector
import co.sorsby.debugtoolkit.data.whois.WhoisClient
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.feature.DnsViewModel
import co.sorsby.debugtoolkit.feature.HttpViewModel
import co.sorsby.debugtoolkit.feature.LanScanViewModel
import co.sorsby.debugtoolkit.feature.NetworkViewModel
import co.sorsby.debugtoolkit.feature.PingViewModel
import co.sorsby.debugtoolkit.feature.PortScanViewModel
import co.sorsby.debugtoolkit.feature.PublicIpViewModel
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.feature.SpeedViewModel
import co.sorsby.debugtoolkit.feature.TlsViewModel
import co.sorsby.debugtoolkit.feature.WhoisViewModel
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
        assertNotNull(koin.get<PingRunner>())
        assertNotNull(koin.get<TracerouteRunner>())
        assertNotNull(koin.get<PortScanner>())
        assertNotNull(koin.get<WhoisClient>())
        assertNotNull(koin.get<PublicIpLookup>())
        assertNotNull(koin.get<LanScanner>())
        assertNotNull(koin.get<JourneyTracker>())
        assertNotNull(koin.get<SettingsViewModel>())
        assertNotNull(koin.get<NetworkViewModel>())
        assertNotNull(koin.get<SpeedViewModel>())
        assertNotNull(koin.get<DnsViewModel>())
        assertNotNull(koin.get<TlsViewModel>())
        assertNotNull(koin.get<HttpViewModel>())
        assertNotNull(koin.get<PingViewModel>())
        assertNotNull(koin.get<PortScanViewModel>())
        assertNotNull(koin.get<WhoisViewModel>())
        assertNotNull(koin.get<PublicIpViewModel>())
        assertNotNull(koin.get<LanScanViewModel>())
    }

    @Test
    fun bottomNavigationOpensToolsCatalog() {
        composeRule.onNodeWithText("Tools").performClick()

        // The same destination labels also exist, off-screen, inside the navigation drawer's
        // (always-composed) content, and the tools list itself is a LazyColumn that only
        // composes on-screen items, so performScrollToNode (which scrolls incrementally until a
        // match appears) is used, scoped to the list's own test tag to avoid the drawer's copy.
        listOf(
            "Certificate inspector",
            "DNS lookup",
            "HTTP inspector",
            "Ping and traceroute",
            "Port scanner",
            "WHOIS lookup",
            "Public IP and location",
            "Local network scanner",
        ).forEach(::assertVisibleInScreenList)
    }

    @Test
    fun networkTabShowsContinuousMonitor() {
        composeRule.onNodeWithText("Network").performClick()
        assertVisibleInScreenList("MONITORING LIVE")
        assertVisibleInScreenList("Connection details")
        assertVisibleInScreenList("Signal and link properties update live as they change")
    }

    @Test
    fun drawerOpensSettings() {
        openDrawerDestination("Settings")
        assertVisibleInScreenList("Appearance")
        assertVisibleInScreenList("Optional analytics")
    }

    @Test
    fun settingsHidesUndecidedConsentOption() {
        openDrawerDestination("Settings")
        composeRule.onNodeWithText("Not decided").assertDoesNotExist()
        assertVisibleInScreenList("Granted")
        assertVisibleInScreenList("Denied")
    }

    @Test
    fun drawerOpensEveryDiagnostic() {
        openDrawerDestination("Speed test")
        assertVisibleInScreenList(composeRule.activity.getString(R.string.speed_intro))

        openDrawerDestination("Certificate inspector")
        assertVisibleInScreenList("Inspect certificate")

        openDrawerDestination("DNS lookup")
        assertVisibleInScreenList("Query DNS")

        openDrawerDestination("HTTP inspector")
        assertVisibleInScreenList("Inspect response")
        assertVisibleInScreenList("Show response body")
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun drawerOpensEveryNewDiagnostic() {
        openDrawerDestination("Ping and traceroute")
        assertVisibleInScreenList(composeRule.activity.getString(R.string.ping_intro))

        openDrawerDestination("Port scanner")
        assertVisibleInScreenList(composeRule.activity.getString(R.string.portscan_intro))

        openDrawerDestination("WHOIS lookup")
        assertVisibleInScreenList(composeRule.activity.getString(R.string.whois_intro))

        openDrawerDestination("Public IP and location")
        assertVisibleInScreenList(composeRule.activity.getString(R.string.publicip_intro))
        assertVisibleInScreenList(composeRule.activity.getString(R.string.publicip_action))

        openDrawerDestination("Local network scanner")
        assertVisibleInScreenList(composeRule.activity.getString(R.string.lanscan_intro))
        assertVisibleInScreenList(composeRule.activity.getString(R.string.lanscan_action))
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

    /**
     * Scrolls the shared screen list until [text] is reachable and asserts it is on screen. CI
     * emulators use a far smaller display than a typical handset, so content that sits above the
     * fold locally is below it there. Scrolling first keeps the assertion about the content being
     * present and reachable rather than about the screen happening to be tall enough.
     */
    private fun assertVisibleInScreenList(text: String): SemanticsNodeInteraction {
        composeRule.onNodeWithTag("screenList").performScrollToNode(hasText(text))
        return composeRule.onNode(hasText(text) and hasAnyAncestor(hasTestTag("screenList")))
            .assertIsDisplayed()
    }

    /**
     * Opens the drawer and activates [label], scrolling the drawer first so the destination is on
     * screen before it is clicked. The drawer is taller than a short display, so its trailing
     * entries are only reachable after scrolling.
     */
    private fun openDrawerDestination(label: String) {
        composeRule.onNodeWithContentDescription("Open navigation").performClick()
        composeRule.onNodeWithTag("drawerContent").performScrollToNode(hasText(label))
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
