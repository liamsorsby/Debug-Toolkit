package co.sorsby.debugtoolkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.ui.components.ScreenList
import co.sorsby.debugtoolkit.ui.components.SectionHeader
import co.sorsby.debugtoolkit.ui.navigation.AppDestination
import co.sorsby.debugtoolkit.ui.navigation.AppDestinations
import co.sorsby.debugtoolkit.ui.theme.BrandGradient
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme

@Composable
fun OverviewScreen(onDestinationSelected: (String) -> Unit) {
    ScreenList {
        item { HeroCard() }
        item {
            SectionHeader(
                stringResource(R.string.overview_tools_title),
                stringResource(R.string.overview_tools_supporting),
            )
        }
        items(AppDestinations.diagnostics.size) { index ->
            val destination = AppDestinations.diagnostics[index]
            ToolCard(destination) { onDestinationSelected(destination.route) }
        }
    }
}

@Composable
fun ToolsScreen(onDestinationSelected: (String) -> Unit) {
    ScreenList {
        item {
            SectionHeader(
                stringResource(R.string.tools_title),
                stringResource(R.string.tools_supporting),
            )
        }
        items(AppDestinations.diagnostics.size) { index ->
            val destination = AppDestinations.diagnostics[index]
            ToolCard(destination) { onDestinationSelected(destination.route) }
        }
    }
}

@Composable
private fun HeroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(BrandGradient.brush())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Default.NetworkCheck,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(12.dp),
            )
        }
        Text(
            stringResource(R.string.overview_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Text(
            stringResource(R.string.overview_hero_body),
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun ToolCard(destination: AppDestination, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    destination.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(stringResource(destination.label), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(destination.description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewScreenPreview() {
    DebugToolkitTheme { OverviewScreen {} }
}

@Preview(showBackground = true)
@Composable
private fun ToolsScreenPreview() {
    DebugToolkitTheme { ToolsScreen {} }
}
