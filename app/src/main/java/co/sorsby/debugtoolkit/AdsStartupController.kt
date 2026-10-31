package co.sorsby.debugtoolkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class AdsStartupController(
    private val adsEnabled: Flow<Boolean>,
    private val consentAllowsAds: Flow<Boolean>,
    private val startAds: suspend () -> Unit,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        combine(adsEnabled, consentAllowsAds) { enabled, consented ->
            enabled && consented
        }.first { it }
        startAds()
    }
}
