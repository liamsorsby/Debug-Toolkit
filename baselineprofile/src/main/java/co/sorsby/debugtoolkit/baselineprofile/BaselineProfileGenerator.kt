package co.sorsby.debugtoolkit.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "co.sorsby.debugtoolkit",
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            device.findObject(By.text("Tools")).click()
            device.waitForIdle()
            device.findObject(By.text("Network")).click()
            device.waitForIdle()
        }
    }
}
