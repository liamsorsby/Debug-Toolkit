package co.sorsby.debugtoolkit

import android.content.Context
import android.util.Log
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal object AdsInitializer {
    fun initialize(
        context: Context,
        settingsRepository: SettingsRepository,
        scope: CoroutineScope,
    ): Boolean {
        AdsStartupController(settingsRepository.settings.map { it.adsEnabled }) {
            withContext(Dispatchers.IO) {
                MobileAds.initialize(context) {
                    Log.d("Ads", "Mobile Ads initialized")
                }
            }
        }.start(scope)
        return true
    }
}
