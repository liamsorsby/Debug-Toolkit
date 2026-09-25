package co.sorsby.debugtoolkit.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette. These tones mirror the app icon and store assets (navy and
// cyan), so the static (non-dynamic) theme reads as intentionally branded
// rather than a default Material scheme with a couple of colors swapped in.
val Blue80 = Color(0xFFA8C7FA)
val Slate80 = Color(0xFFBBC7DB)
val Cyan80 = Color(0xFF82D5E8)

val Blue40 = Color(0xFF235FA6)
val Slate40 = Color(0xFF4E6078)
val Cyan40 = Color(0xFF00677A)

val DarkBackground = Color(0xFF0F141A)
val DarkSurface = Color(0xFF171C23)
val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)

// Full tonal extension of the brand palette, used to fill in every Material 3
// color role (containers, surface tiers, outlines, inverse colors) so both
// light and dark themes look deliberately designed instead of falling back to
// the Material baseline purple for anything not explicitly set.

// Light theme roles.
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD3E3FF)
val LightOnPrimaryContainer = Color(0xFF001B3D)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD4E4FA)
val LightOnSecondaryContainer = Color(0xFF0A1E30)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFA0EFFF)
val LightOnTertiaryContainer = Color(0xFF001F26)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightOnBackground = Color(0xFF1A1C1E)
val LightOnSurface = Color(0xFF1A1C1E)
val LightSurfaceVariant = Color(0xFFDFE2EB)
val LightOnSurfaceVariant = Color(0xFF43474E)
val LightOutline = Color(0xFF73777F)
val LightOutlineVariant = Color(0xFFC3C7CF)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF1F4F9)
val LightSurfaceContainer = Color(0xFFEBEEF4)
val LightSurfaceContainerHigh = Color(0xFFE5E9F0)
val LightSurfaceContainerHighest = Color(0xFFDFE3EA)
val LightInverseSurface = Color(0xFF2E3135)
val LightInverseOnSurface = Color(0xFFEFF0F5)
val LightInversePrimary = Blue80

// Dark theme roles.
val DarkOnPrimary = Color(0xFF063061)
val DarkPrimaryContainer = Color(0xFF0A4788)
val DarkOnPrimaryContainer = Color(0xFFD3E3FF)
val DarkOnSecondary = Color(0xFF24384C)
val DarkSecondaryContainer = Color(0xFF3A4D63)
val DarkOnSecondaryContainer = Color(0xFFD4E4FA)
val DarkOnTertiary = Color(0xFF003640)
val DarkTertiaryContainer = Color(0xFF004E5E)
val DarkOnTertiaryContainer = Color(0xFFB8EAFF)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkOnBackground = Color(0xFFE1E2E8)
val DarkOnSurface = Color(0xFFE1E2E8)
val DarkSurfaceVariant = Color(0xFF43474E)
val DarkOnSurfaceVariant = Color(0xFFC3C7CF)
val DarkOutline = Color(0xFF8D9199)
val DarkOutlineVariant = Color(0xFF43474E)
val DarkSurfaceContainerLowest = Color(0xFF0A0E13)
val DarkSurfaceContainerLow = Color(0xFF191E25)
val DarkSurfaceContainer = Color(0xFF1D222A)
val DarkSurfaceContainerHigh = Color(0xFF272C34)
val DarkSurfaceContainerHighest = Color(0xFF32373F)
val DarkInverseSurface = Color(0xFFE1E2E8)
val DarkInverseOnSurface = Color(0xFF2E3135)
val DarkInversePrimary = Blue40