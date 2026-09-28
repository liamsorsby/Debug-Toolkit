package co.sorsby.debugtoolkit.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import co.sorsby.debugtoolkit.BuildConfig
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError


@Composable
fun AdsComponent(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val adSize = remember {
        AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, 360)
    }

    val adView = remember {
        AdView(context)
    }

    DisposableEffect(adView) {
        val adRequest = BannerAdRequest.Builder(
            BuildConfig.AD_UNIT_ID,
            adSize
        ).build()

        adView.loadAd(
            adRequest,
            object : AdLoadCallback<BannerAd> {

                override fun onAdLoaded(
                    ad: BannerAd
                ) {
                    Log.d("AdsComponent", "Banner ad loaded")
                }

                override fun onAdFailedToLoad(
                    adError: LoadAdError
                ) {
                    Log.e(
                        "AdsComponent",
                        "Failed to load banner: $adError"
                    )
                }
            }
        )

        onDispose {
            adView.destroy()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(),
            factory = {
                adView
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticComponentsPreview() {
    DebugToolkitTheme {
        AdsComponent()
    }
}
