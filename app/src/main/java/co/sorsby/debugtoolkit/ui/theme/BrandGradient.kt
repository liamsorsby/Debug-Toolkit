package co.sorsby.debugtoolkit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

/**
 * Fixed brand gradient used on a small number of signature surfaces (top bar, navigation
 * drawer header, the Overview hero card, and the consent gate) so the app reads as
 * intentionally branded even when Material You dynamic color is active and has replaced every
 * other role with wallpaper-derived tones. Deliberately built from [Color] constants rather than
 * `MaterialTheme.colorScheme`, since the whole point is to stay constant regardless of theme.
 */
object BrandGradient {
    @Composable
    fun brush(): Brush {
        // The active color scheme's surface luminance reflects whichever mode is actually
        // rendering (system default or a manual override from Settings), so the gradient always
        // matches what's on screen rather than assuming the device's raw system setting.
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val (start, end) = if (isDarkTheme) {
            BrandGradientDarkStart to BrandGradientDarkEnd
        } else {
            BrandGradientLightStart to BrandGradientLightEnd
        }
        return Brush.linearGradient(listOf(start, end))
    }
}
