package co.sorsby.debugtoolkit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.sorsby.debugtoolkit.R

/** Overlays [content] with a persistent "restart to update" prompt when [visible]. */
@Composable
fun UpdateDownloadedHost(
    visible: Boolean,
    onRestart: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (visible) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    // Clears the app's bottom navigation bar.
                    .padding(start = 12.dp, end = 12.dp, bottom = 88.dp)
                    .testTag("update_downloaded_snackbar"),
                action = {
                    TextButton(onClick = onRestart) {
                        Text(stringResource(R.string.update_downloaded_action))
                    }
                },
            ) {
                Text(stringResource(R.string.update_downloaded_message))
            }
        }
    }
}
