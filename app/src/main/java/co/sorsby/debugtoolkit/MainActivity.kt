package co.sorsby.debugtoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
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
