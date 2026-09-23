package co.sorsby.debugtoolkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.sorsby.debugtoolkit.R
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.ui.navigation.AppDestination
import co.sorsby.debugtoolkit.ui.navigation.AppDestinations
import co.sorsby.debugtoolkit.ui.screens.AboutScreen
import co.sorsby.debugtoolkit.ui.screens.DnsRoute
import co.sorsby.debugtoolkit.ui.screens.HttpRoute
import co.sorsby.debugtoolkit.ui.screens.NetworkRoute
import co.sorsby.debugtoolkit.ui.screens.OverviewScreen
import co.sorsby.debugtoolkit.ui.screens.SettingsRoute
import co.sorsby.debugtoolkit.ui.screens.SpeedRoute
import co.sorsby.debugtoolkit.ui.screens.TlsRoute
import co.sorsby.debugtoolkit.ui.screens.ToolsScreen
import co.sorsby.debugtoolkit.ui.theme.DebugToolkitTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolkitApp(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: AppDestinations.Overview.route
    val title = AppDestinations.all
        .firstOrNull { it.route == route }
        ?.label
        ?: R.string.app_name

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                selectedRoute = route,
                onDestinationSelected = { destination ->
                    navController.navigateSingle(destination.route)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(title)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.navigation_open),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                AppBottomBar(
                    selectedRoute = route,
                    onDestinationSelected = { navController.navigateSingle(it.route) },
                )
            },
        ) { padding ->
            AppNavHost(navController, padding, settingsViewModel)
        }
    }
}

@Composable
private fun AppDrawer(
    selectedRoute: String,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.app_tagline),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(12.dp))
        AppDestinations.drawer.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.label)) },
                selected = selectedRoute == destination.route,
                icon = { Icon(destination.icon, contentDescription = null) },
                onClick = { onDestinationSelected(destination) },
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    selectedRoute: String,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar {
        AppDestinations.bottom.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    padding: PaddingValues,
    settingsViewModel: SettingsViewModel,
) {
    val navigate: (String) -> Unit = navController::navigateSingle
    NavHost(
        navController = navController,
        startDestination = AppDestinations.Overview.route,
        modifier = Modifier.padding(padding),
    ) {
        composable(AppDestinations.Overview.route) { OverviewScreen(navigate) }
        composable(AppDestinations.Network.route) { NetworkRoute() }
        composable(AppDestinations.Tools.route) { ToolsScreen(navigate) }
        composable(AppDestinations.Speed.route) { SpeedRoute(settingsViewModel) }
        composable(AppDestinations.Tls.route) { TlsRoute() }
        composable(AppDestinations.Dns.route) { DnsRoute() }
        composable(AppDestinations.Http.route) { HttpRoute() }
        composable(AppDestinations.Settings.route) { SettingsRoute(settingsViewModel) }
        composable(AppDestinations.About.route) { AboutScreen() }
    }
}

private fun NavHostController.navigateSingle(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) { saveState = true }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDrawerPreview() {
    DebugToolkitTheme {
        AppDrawer(
            selectedRoute = AppDestinations.Speed.route,
            onDestinationSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarPreview() {
    DebugToolkitTheme {
        AppBottomBar(
            selectedRoute = AppDestinations.Overview.route,
            onDestinationSelected = {},
        )
    }
}
