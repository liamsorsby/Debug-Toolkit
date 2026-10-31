package co.sorsby.debugtoolkit

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.BuildConfig
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.AdsConsentManager
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.review.ReviewPromptCoordinator
import co.sorsby.debugtoolkit.update.SoftUpdatePolicy
import co.sorsby.debugtoolkit.update.SoftUpdatePromptStore
import co.sorsby.debugtoolkit.update.VersionPolicy
import co.sorsby.debugtoolkit.update.VersionPolicyDecoder
import co.sorsby.debugtoolkit.update.VersionPolicyRepository
import co.sorsby.debugtoolkit.ui.DebugToolkitApp
import co.sorsby.debugtoolkit.ui.components.UpdateDownloadedHost
import co.sorsby.debugtoolkit.ui.screens.ConsentGateScreen
import co.sorsby.debugtoolkit.ui.screens.UpdateRequiredScreen
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val adsConsentManager: AdsConsentManager by inject()
    private val updateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val versionPolicyRepository: VersionPolicyRepository by inject()
    private val reviewPromptCoordinator: ReviewPromptCoordinator by inject()
    private val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(this) }
    private val softUpdatePromptStore by lazy { SoftUpdatePromptStore(this) }
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                openUpdateUrl(currentUpdateUrl)
            }
        }

    // Declining a flexible update is allowed, so the result needs no handling.
    private val flexibleUpdateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private var currentUpdateUrl: String = PLAY_STORE_URL
    private var updateDownloaded by mutableStateOf(false)
    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            updateDownloaded = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The default icon exit animation relies on a ValueAnimator completion callback that
        // never fires when animations are globally disabled (Android's "disable animations"
        // developer option, and every instrumented test environment, including the CI emulator).
        // Without this listener the splash view is left attached indefinitely once the keep-on-
        // screen condition clears below, which blocks Espresso/Compose idle detection forever.
        // Removing the view immediately is a safe no-op visually, since disabled-animation
        // environments would have skipped the animation anyway.
        splashScreen.setOnExitAnimationListener { splashScreenView -> splashScreenView.remove() }
        observeReviewRequests()
        setContent {
            val versionPolicyRepository: VersionPolicyRepository = koinInject()
            val versionPolicy by versionPolicyRepository.policy.collectAsStateWithLifecycle()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val isReady by settingsViewModel.isReady.collectAsStateWithLifecycle()
            LaunchedEffect(versionPolicyRepository) {
                versionPolicyRepository.refresh()
            }
            // Keep the branded splash on screen until the persisted settings have actually
            // loaded, so a returning user's saved consent choice is never replaced for a frame
            // by the first-launch gate while AppSettings()'s placeholder default is showing.
            splashScreen.setKeepOnScreenCondition { !isReady }
            LaunchedEffect(isReady, settings.analyticsConsent) {
                if (isReady) {
                    adsConsentManager.requestConsent(
                        activity = this@MainActivity,
                        showForm = settings.analyticsConsent != AnalyticsConsent.UNSET,
                    )
                }
            }
            DebugToolkitTheme(
                darkTheme = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> null
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                val policy = versionPolicy
                val requiresUpdate = VersionPolicyDecoder.isUpdateRequired(
                    installedVersionCode = BuildConfig.VERSION_CODE,
                    policy = policy,
                )
                if (requiresUpdate && policy != null) {
                    UpdateRequiredScreen(onUpdate = { startUpdateOrOpenStore(policy) })
                } else if (settings.analyticsConsent == AnalyticsConsent.UNSET) {
                    ConsentGateScreen(
                        onAccept = { settingsViewModel.setConsent(AnalyticsConsent.GRANTED) },
                        onDecline = { settingsViewModel.setConsent(AnalyticsConsent.DENIED) },
                    )
                } else {
                    UpdateDownloadedHost(
                        visible = updateDownloaded,
                        onRestart = { updateManager.completeUpdate() },
                    ) {
                        DebugToolkitApp(
                            settingsViewModel = settingsViewModel,
                            onShowPrivacyOptions = {
                                adsConsentManager.showPrivacyOptions(this@MainActivity)
                            },
                        )
                    }
                }
            }

        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (CallKeyGuard.shouldIntercept(Build.VERSION.SDK_INT, keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!CallKeyGuard.shouldIntercept(Build.VERSION.SDK_INT, keyCode)) {
            return super.onKeyUp(keyCode, event)
        }
        if (!event.isCanceled) {
            try {
                startActivity(Intent(Intent.ACTION_CALL_BUTTON))
            } catch (exception: ActivityNotFoundException) {
                Log.d(TAG, "No app handles the call button", exception)
            }
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        updateManager.registerListener(installStateListener)
        checkForSoftUpdate()
    }

    private fun observeReviewRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                reviewPromptCoordinator.requests.collect { launchReview() }
            }
        }
    }

    private fun launchReview() {
        reviewManager.requestReviewFlow().addOnCompleteListener(this) { request ->
            if (!request.isSuccessful) {
                // Expected for non-Play installs or when Play is unavailable.
                Log.d(TAG, "In-app review unavailable", request.exception)
                return@addOnCompleteListener
            }
            // Play doesn't report whether a dialog was shown or a review left, so the app
            // carries on regardless of the outcome.
            reviewManager.launchReviewFlow(this, request.result)
        }
    }

    override fun onStop() {
        updateManager.unregisterListener(installStateListener)
        super.onStop()
    }

    private fun checkForSoftUpdate() {
        updateManager.appUpdateInfo
            .addOnSuccessListener(this) { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    updateDownloaded = true
                } else {
                    maybeStartFlexibleUpdate(info)
                }
            }
            .addOnFailureListener(this) { exception ->
                // Expected for sideloaded or non-Play installs.
                Log.d(TAG, "Soft update check unavailable", exception)
            }
    }

    private fun maybeStartFlexibleUpdate(info: AppUpdateInfo) {
        // The hard sunset gate owns the update UX when this version is unsupported.
        if (
            VersionPolicyDecoder.isUpdateRequired(
                installedVersionCode = BuildConfig.VERSION_CODE,
                policy = versionPolicyRepository.policy.value,
            )
        ) {
            return
        }
        val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        val now = System.currentTimeMillis()
        val shouldPrompt = SoftUpdatePolicy.shouldPrompt(
            updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            flexibleAllowed = info.isUpdateTypeAllowed(options),
            stalenessDays = info.clientVersionStalenessDays(),
            nowMs = now,
            lastPromptAtMs = softUpdatePromptStore.lastPromptAtMs,
        )
        if (shouldPrompt && updateManager.startUpdateFlowForResult(info, flexibleUpdateLauncher, options)) {
            softUpdatePromptStore.lastPromptAtMs = now
        }
    }

    private fun startUpdateOrOpenStore(policy: VersionPolicy) {
        currentUpdateUrl = policy.updateUrl
        updateManager.appUpdateInfo
            .addOnSuccessListener(this) { info ->
                val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                val updateAvailable =
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        info.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if (
                    !updateAvailable ||
                    !info.isUpdateTypeAllowed(options) ||
                    !updateManager.startUpdateFlowForResult(info, updateLauncher, options)
                ) {
                    openUpdateUrl(policy.updateUrl)
                }
            }
            .addOnFailureListener(this) { exception ->
                Log.w(TAG, "Play update flow is unavailable; opening update URL", exception)
                openUpdateUrl(policy.updateUrl)
            }
    }

    private fun openUpdateUrl(url: String) {
        val parsed = VersionPolicyDecoder.normalizeHttpsUrl(url) ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(parsed)))
        } catch (exception: ActivityNotFoundException) {
            Log.e(TAG, "No app can open the update URL", exception)
            Toast.makeText(this, R.string.update_open_store_failed, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=co.sorsby.debugtoolkit"
    }
}
