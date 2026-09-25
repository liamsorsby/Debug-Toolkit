package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.ToolIntroCard
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme

/**
 * Placeholder entry point for the PortScan tool. The navigation destination is wired up ahead
 * of the tool implementation so the drawer and Tools screen reflect the final structure; this
 * body is replaced with the real feature in a follow-up change.
 */
@Composable
fun PortScanRoute() {
    PortScanScreen()
}

@Composable
fun PortScanScreen() {
    ScreenList {
        item {
            ToolIntroCard(
                icon = Icons.Default.Radar,
                description = stringResource(R.string.tool_coming_soon),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PortScanScreenPreview() {
    DebugToolkitTheme { PortScanScreen() }
}
