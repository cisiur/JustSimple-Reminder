package com.justsimple.reminder.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.justsimple.reminder.R

/**
 * Displays an AdMob adaptive banner at the bottom of the screen (free tier only).
 *
 * Lifecycle is tied to the host Composable — the AdView is paused / resumed /
 * destroyed alongside the Activity to prevent memory leaks and comply with
 * AdMob's lifecycle requirements.
 *
 * Ad unit ID comes from strings.xml so it can be overridden per build variant
 * without touching code. Replace the test ID with your real one before release:
 *   res/values/strings.xml → admob_banner_unit_id
 */
@Composable
fun AdBannerView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = context.getString(R.string.admob_banner_unit_id)
            loadAd(AdRequest.Builder().build())
        }
    }

    // Mirror Activity lifecycle events into the AdView so ads are properly
    // paused when the app goes to background and destroyed on exit.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME  -> adView.resume()
                Lifecycle.Event.ON_PAUSE   -> adView.pause()
                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else                       -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = Modifier.fillMaxWidth(),
    )
}
