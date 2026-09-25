package co.sorsby.debugtoolkit.telemetry

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsConsentPolicyTest {
    @Test
    fun `only an explicit grant enables journey collection`() {
        AnalyticsConsent.entries.forEach { consent ->
            assertEquals(
                "consent $consent",
                consent == AnalyticsConsent.GRANTED,
                AnalyticsConsentPolicy.journeyCollectionEnabled(consent),
            )
        }
    }

    @Test
    fun `an unanswered consent gate does not enable collection`() {
        assertEquals(
            false,
            AnalyticsConsentPolicy.journeyCollectionEnabled(AnalyticsConsent.UNSET),
        )
    }

    @Test
    fun `analytics storage follows the users choice`() {
        assertEquals(
            ConsentStatus.GRANTED,
            AnalyticsConsentPolicy.consentSettings(true)[ConsentType.ANALYTICS_STORAGE],
        )
        assertEquals(
            ConsentStatus.DENIED,
            AnalyticsConsentPolicy.consentSettings(false)[ConsentType.ANALYTICS_STORAGE],
        )
    }

    @Test
    fun `advertising consent is denied even when analytics is granted`() {
        val advertising = listOf(
            ConsentType.AD_STORAGE,
            ConsentType.AD_USER_DATA,
            ConsentType.AD_PERSONALIZATION,
        )

        listOf(true, false).forEach { granted ->
            val settings = AnalyticsConsentPolicy.consentSettings(granted)
            advertising.forEach { type ->
                assertEquals("$type with granted=$granted", ConsentStatus.DENIED, settings[type])
            }
        }
    }

    @Test
    fun `every consent signal is declared so none is left to default`() {
        val settings = AnalyticsConsentPolicy.consentSettings(true)

        assertEquals(
            setOf(
                ConsentType.ANALYTICS_STORAGE,
                ConsentType.AD_STORAGE,
                ConsentType.AD_USER_DATA,
                ConsentType.AD_PERSONALIZATION,
            ),
            settings.keys,
        )
    }
}
