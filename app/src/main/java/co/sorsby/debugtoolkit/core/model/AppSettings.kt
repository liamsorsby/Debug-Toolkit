package co.sorsby.debugtoolkit.core.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val analyticsConsent: AnalyticsConsent = AnalyticsConsent.UNSET,
    val cloudflareDisclosureAccepted: Boolean = false,
)
