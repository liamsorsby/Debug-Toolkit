package co.sorsby.debugtoolkit.ui.components

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import co.sorsby.debugtoolkit.BuildConfig
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdsComponent(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val adWidth = maxWidth.value.toInt()
        val adView = remember(context, adWidth) {
            AdView(context).apply {
                adUnitId = BuildConfig.AD_UNIT_ID
                setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth))
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdsComponent", "Banner ad loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdsComponent", "Failed to load banner: $error")
                    }
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().testTag("dnsAdBanner"),
            factory = { adView },
        )
        DisposableEffect(adView) {
            adView.loadAd(AdRequest.Builder().build())
            onDispose { adView.destroy() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticComponentsPreview() {
    DebugToolkitTheme {
        AdsComponent()
    }
}
