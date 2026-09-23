package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.ui.components.InformationCard
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme

@Composable
fun AboutScreen() {
    ScreenList {
        item {
            InformationCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.about_privacy),
                body = stringResource(R.string.about_privacy_body),
            )
        }
        item {
            InformationCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_measurements),
                body = stringResource(R.string.about_measurements_body),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    DebugToolkitTheme { AboutScreen() }
}
