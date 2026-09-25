package co.sorsby.debugtoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.ThemeMode
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.ui.DebugToolkitApp
import co.sorsby.debugtoolkit.ui.screens.ConsentGateScreen
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
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
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val isReady by settingsViewModel.isReady.collectAsStateWithLifecycle()
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
                if (settings.analyticsConsent == AnalyticsConsent.UNSET) {
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
}
