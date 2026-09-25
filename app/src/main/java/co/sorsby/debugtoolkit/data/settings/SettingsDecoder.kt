package co.sorsby.debugtoolkit.data.settings

import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.ThemeMode

/**
 * Turns the stored preference values into [AppSettings]. Persisted enums are read back by name,
 * so anything the current build no longer recognises (a setting written by a newer version, or
 * a corrupt store) has to fall back to the default rather than fail. Kept separate from
 * [DataStoreSettingsRepository] so those fallbacks are unit testable without DataStore.
 */
object SettingsDecoder {
    fun decode(
        themeMode: String?,
        analyticsConsent: String?,
        cloudflareDisclosureAccepted: Boolean?,
    ): AppSettings = AppSettings(
        themeMode = themeMode?.toEnumOrNull() ?: ThemeMode.SYSTEM,
        analyticsConsent = analyticsConsent?.toEnumOrNull() ?: AnalyticsConsent.UNSET,
        cloudflareDisclosureAccepted = cloudflareDisclosureAccepted ?: false,
    )

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { it.name == this }
}
