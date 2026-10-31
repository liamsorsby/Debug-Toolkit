package co.sorsby.debugtoolkit

import android.view.KeyEvent

/**
 * On Android 12+, an unhandled CALL key falls through to PhoneFallbackEventHandler, which
 * broadcasts CLOSE_SYSTEM_DIALOGS. Apps targeting Android 12+ lack permission for that
 * broadcast, so the platform throws a SecurityException and the app crashes. Consuming the key
 * in the activity keeps it away from the fallback handler.
 */
internal object CallKeyGuard {
    private const val ANDROID_12 = 31

    fun shouldIntercept(sdkInt: Int, keyCode: Int): Boolean =
        sdkInt >= ANDROID_12 && keyCode == KeyEvent.KEYCODE_CALL
}
