package co.sorsby.debugtoolkit.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AnalyticsConsent {
    UNSET,
    DENIED,
    GRANTED,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val analyticsConsent: AnalyticsConsent = AnalyticsConsent.UNSET,
    val cloudflareDisclosureAccepted: Boolean = false,
)
