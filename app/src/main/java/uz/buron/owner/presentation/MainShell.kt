package uz.buron.owner.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.buron.owner.R
import uz.buron.owner.ui.components.OfflineBanner

enum class MainTab(val route: String, val titleRes: Int) {
    Dashboard("dashboard", R.string.nav_dashboard),
    Bookings("bookings", R.string.nav_bookings),
    Venues("venues", R.string.nav_venues),
    Profile("profile", R.string.nav_profile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    showOfflineBanner: Boolean = false,
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = hiltViewModel(),
    content: @Composable (Modifier) -> Unit
) {
    val pendingCount by appViewModel.pendingBookingsCount.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentTab.titleRes)) },
                actions = {
                    if (pendingCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(pendingCount.toString()) }
                            },
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = stringResource(R.string.pending_bookings_badge)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MainTab.Dashboard -> Icons.Default.Dashboard
                                    MainTab.Bookings -> Icons.AutoMirrored.Filled.EventNote
                                    MainTab.Venues -> Icons.Default.Business
                                    MainTab.Profile -> Icons.Default.Person
                                },
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = { Text(stringResource(tab.titleRes)) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showOfflineBanner) {
                OfflineBanner()
            }
            content(Modifier.fillMaxSize())
        }
    }
}
