package co.sorsby.debugtoolkit

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference

internal class AdsConsentManager(context: Context) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val mutableCanRequestAds = MutableStateFlow(false)
    private val mutablePrivacyOptionsRequired = MutableStateFlow(false)
    private val mutableMobileAdsInitialized = MutableStateFlow(false)
    private var hasRequestedConsentInfo = false
    private var consentInfoUpdateComplete = false
    private var consentInfoUpdateFailed = false
    private var shouldShowConsentForm = false
    private var hasStartedConsentForm = false
    private var activityForConsentForm: WeakReference<Activity>? = null

    val canRequestAds: StateFlow<Boolean> = mutableCanRequestAds
    val privacyOptionsRequired: StateFlow<Boolean> = mutablePrivacyOptionsRequired
    val mobileAdsInitialized: StateFlow<Boolean> = mutableMobileAdsInitialized

    fun onMobileAdsInitialized() {
        mutableMobileAdsInitialized.value = true
    }

    fun requestConsent(activity: Activity, showForm: Boolean) {
        activityForConsentForm = WeakReference(activity)
        shouldShowConsentForm = shouldShowConsentForm || showForm
        if (hasRequestedConsentInfo) {
            if (consentInfoUpdateComplete) {
                handleConsentInfoReady()
            }
            return
        }

        hasRequestedConsentInfo = true
        mutableCanRequestAds.value = false
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                consentInfoUpdateComplete = true
                updatePrivacyOptionsRequirement()
                handleConsentInfoReady()
            },
            { error ->
                consentInfoUpdateComplete = true
                consentInfoUpdateFailed = true
                Log.w(TAG, "Failed to update consent information: $error")
                updatePrivacyOptionsRequirement()
                handleConsentInfoReady()
            },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Failed to show privacy options: $error")
            }
            updatePrivacyOptionsRequirement()
            updateConsentToRequestAds()
        }
    }

    private fun updatePrivacyOptionsRequirement() {
        mutablePrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun updateConsentToRequestAds() {
        mutableCanRequestAds.value = consentInformation.canRequestAds()
    }

    private fun handleConsentInfoReady() {
        if (!shouldShowConsentForm || hasStartedConsentForm) return
        if (consentInfoUpdateFailed) {
            updateConsentToRequestAds()
            return
        }
        val activity = activityForConsentForm?.get() ?: return
        hasStartedConsentForm = true
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Failed to show consent form: $error")
            }
            updateConsentToRequestAds()
        }
    }

    private companion object {
        const val TAG = "AdsConsent"
    }
}
