package co.sorsby.debugtoolkit.data.settings

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDecoderTest {
    @Test
    fun `an empty store decodes to the defaults`() {
        val settings = SettingsDecoder.decode(null, null, null)

        assertEquals(AppSettings(), settings)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AnalyticsConsent.UNSET, settings.analyticsConsent)
        assertEquals(false, settings.cloudflareDisclosureAccepted)
    }

    @Test
    fun `stored values are decoded back to their enum`() {
        val settings = SettingsDecoder.decode(
            themeMode = ThemeMode.DARK.name,
            analyticsConsent = AnalyticsConsent.DENIED.name,
            cloudflareDisclosureAccepted = true,
        )

        assertEquals(AppSettings(ThemeMode.DARK, AnalyticsConsent.DENIED, true), settings)
    }

    @Test
    fun `every theme mode survives a round trip`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, SettingsDecoder.decode(mode.name, null, null).themeMode)
        }
    }

    @Test
    fun `every consent choice survives a round trip`() {
        AnalyticsConsent.entries.forEach { consent ->
            assertEquals(
                consent,
                SettingsDecoder.decode(null, consent.name, null).analyticsConsent,
            )
        }
    }

    @Test
    fun `an unrecognised stored value falls back to the default`() {
        val settings = SettingsDecoder.decode(
            themeMode = "SEPIA",
            analyticsConsent = "PARTIAL",
            cloudflareDisclosureAccepted = null,
        )

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AnalyticsConsent.UNSET, settings.analyticsConsent)
    }

    @Test
    fun `decoding is case sensitive so a mismatched name is not silently accepted`() {
        assertEquals(ThemeMode.SYSTEM, SettingsDecoder.decode("dark", null, null).themeMode)
        assertEquals(ThemeMode.SYSTEM, SettingsDecoder.decode("", null, null).themeMode)
    }
}
