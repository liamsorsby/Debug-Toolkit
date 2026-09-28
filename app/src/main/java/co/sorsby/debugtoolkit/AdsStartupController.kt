package co.sorsby.debugtoolkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class AdsStartupController(
    private val adsEnabled: Flow<Boolean>,
    private val startAds: suspend () -> Unit,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        adsEnabled.first { it }
        startAds()
    }
}
