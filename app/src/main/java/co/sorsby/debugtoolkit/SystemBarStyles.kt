package co.sorsby.debugtoolkit

import android.graphics.Color
import androidx.activity.SystemBarStyle

/**
 * System bar styles for the edge-to-edge window (enforced from Android 15 / SDK 35).
 *
 * The default `enableEdgeToEdge()` style follows the *system* dark mode, which gets two things
 * wrong here: the status bar icons turn dark in light mode even though the main app's top bar
 * is always the dark brand gradient, and the in-app Light/Dark override in Settings is ignored.
 */
internal data class SystemBarStyles(
    val statusBar: SystemBarStyle,
    val navigationBar: SystemBarStyle,
) {
    companion object {
        // Same scrims androidx.activity uses by default, kept for three-button navigation where
        // the bar draws over content and needs a translucent background for contrast.
        private val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        private val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

        fun forScreen(useDarkTheme: Boolean, showsBrandTopBar: Boolean): SystemBarStyles {
            val statusBar = if (useDarkTheme || showsBrandTopBar) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
            val navigationBar = if (useDarkTheme) {
                SystemBarStyle.dark(DarkScrim)
            } else {
                SystemBarStyle.light(LightScrim, DarkScrim)
            }
            return SystemBarStyles(statusBar, navigationBar)
        }
    }
}
