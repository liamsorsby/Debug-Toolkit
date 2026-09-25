package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme

/**
 * Placeholder entry point for the Ping tool. The navigation destination is wired up ahead
 * of the tool implementation so the drawer and Tools screen reflect the final structure; this
 * body is replaced with the real feature in a follow-up change.
 */
@Composable
fun PingRoute() {
    PingScreen()
}

@Composable
fun PingScreen() {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.NetworkPing,
                description = stringResource(R.string.tool_coming_soon),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PingScreenPreview() {
    DebugToolkitTheme { PingScreen() }
}
