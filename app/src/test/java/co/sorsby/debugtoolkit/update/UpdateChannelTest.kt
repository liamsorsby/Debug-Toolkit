package co.sorsby.debugtoolkit.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateChannelTest {
    @Test
    fun playStoreInstallUsesPlayUpdateChannel() {
        assertEquals(
            UpdateChannel.PLAY,
            UpdateChannel.fromInstallerPackageName("com.android.vending"),
        )
    }

    @Test
    fun otherAndUnknownInstallersUseAlternateUpdateChannel() {
        assertEquals(UpdateChannel.OTHER, UpdateChannel.fromInstallerPackageName(null))
        assertEquals(
            UpdateChannel.OTHER,
            UpdateChannel.fromInstallerPackageName("com.google.android.packageinstaller"),
        )
    }
}
