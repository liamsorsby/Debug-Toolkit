package co.sorsby.debugtoolkit

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallKeyGuardTest {
    @Test
    fun `intercepts the call key on Android 12 and later`() {
        assertTrue(CallKeyGuard.shouldIntercept(31, KeyEvent.KEYCODE_CALL))
        assertTrue(CallKeyGuard.shouldIntercept(37, KeyEvent.KEYCODE_CALL))
    }

    @Test
    fun `leaves the call key to the platform before Android 12`() {
        assertFalse(CallKeyGuard.shouldIntercept(30, KeyEvent.KEYCODE_CALL))
    }

    @Test
    fun `never intercepts other keys`() {
        assertFalse(CallKeyGuard.shouldIntercept(31, KeyEvent.KEYCODE_BACK))
        assertFalse(CallKeyGuard.shouldIntercept(37, KeyEvent.KEYCODE_ENTER))
    }
}
