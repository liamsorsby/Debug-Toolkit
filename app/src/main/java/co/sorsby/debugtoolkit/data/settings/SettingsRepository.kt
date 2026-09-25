package co.sorsby.debugtoolkit.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import co.sorsby.debugtoolkit.core.model.AnalyticsConsent
import co.sorsby.debugtoolkit.core.model.AppSettings
import co.sorsby.debugtoolkit.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAnalyticsConsent(consent: AnalyticsConsent)
    suspend fun acceptCloudflareDisclosure()
}

private val Context.dataStore by preferencesDataStore("settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    override val settings: Flow<AppSettings> = context.dataStore.data.map { values ->
        SettingsDecoder.decode(
            themeMode = values[THEME],
            analyticsConsent = values[CONSENT],
            cloudflareDisclosureAccepted = values[CLOUDFLARE_DISCLOSURE],
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun setAnalyticsConsent(consent: AnalyticsConsent) {
        context.dataStore.edit { it[CONSENT] = consent.name }
    }

    override suspend fun acceptCloudflareDisclosure() {
        context.dataStore.edit { it[CLOUDFLARE_DISCLOSURE] = true }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val CONSENT = stringPreferencesKey("analytics_consent")
        val CLOUDFLARE_DISCLOSURE = booleanPreferencesKey("cloudflare_disclosure")
    }
}
