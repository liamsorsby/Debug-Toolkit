package co.sorsby.debugtoolkit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.sorsby.debugtoolkit.ui.screens.UpdateRequiredScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateRequiredScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun updateRequiredScreenOnlyOffersUpdateAction() {
        var updateRequested = false
        composeRule.setContent {
            UpdateRequiredScreen(onUpdate = { updateRequested = true }, versionName = "1.2.3")
        }

        composeRule.onNodeWithText("Update required").assertIsDisplayed()
        composeRule.onNodeWithText("This version of Debug Toolkit is no longer supported. Update the app to continue.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("v1.2.3").assertIsDisplayed()
        composeRule.onNodeWithText("Update now").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Continue without updating").assertDoesNotExist()

        assertTrue(updateRequested)
    }
}
