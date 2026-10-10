package co.sorsby.debugtoolkit

import android.app.Activity
import android.os.Bundle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.BuildConfig
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.update.VersionPolicy
import co.sorsby.debugtoolkit.update.VersionPolicyDecoder
import co.sorsby.debugtoolkit.update.VersionPolicyRepository
import co.sorsby.debugtoolkit.ui.DebugToolkitApp
import co.sorsby.debugtoolkit.ui.screens.ConsentGateScreen
import co.sorsby.debugtoolkit.ui.screens.UpdateRequiredScreen
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val updateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                openUpdateUrl(currentUpdateUrl)
            }
        }
    private var currentUpdateUrl: String = PLAY_STORE_URL

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
                    DebugToolkitApp(settingsViewModel)
                }
            }

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
