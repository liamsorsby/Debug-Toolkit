package co.sorsby.debugtoolkit.telemetry

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType

/**
 * Decides what the app is allowed to report for a given consent choice. Extracted from
 * [FirebaseJourneyTracker] so the rules that keep the app compliant are asserted directly
 * rather than only through the Firebase SDK, which cannot run on the unit test JVM.
 */
object AnalyticsConsentPolicy {
    /**
     * Behavioural reporting requires an explicit opt in, so anything other than a granted
     * choice, including the unset state seen before the consent gate is answered, disables it.
     */
    fun journeyCollectionEnabled(consent: AnalyticsConsent): Boolean =
        consent == AnalyticsConsent.GRANTED

    /**
     * The consent signals sent to Firebase. Analytics storage follows the user's choice; the
     * advertising signals are always denied because the app never runs ads or shares data for
     * advertising, and that must not change silently with a consent change.
     */
    fun consentSettings(analyticsGranted: Boolean): Map<ConsentType, ConsentStatus> = mapOf(
        ConsentType.ANALYTICS_STORAGE to analyticsGranted.toConsentStatus(),
        ConsentType.AD_STORAGE to ConsentStatus.DENIED,
        ConsentType.AD_USER_DATA to ConsentStatus.DENIED,
        ConsentType.AD_PERSONALIZATION to ConsentStatus.DENIED,
    )

    private fun Boolean.toConsentStatus(): ConsentStatus =
        if (this) ConsentStatus.GRANTED else ConsentStatus.DENIED
}
