package uz.buron.owner.presentation.venues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.buron.owner.R
import uz.buron.owner.ui.components.EmptyState
import uz.buron.owner.ui.components.OfflineBanner
import uz.buron.owner.ui.components.VenueOwnerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuesScreen(
    onNavigateToAddVenue: () -> Unit,
    onNavigateToVenueDetail: (String) -> Unit,
    onNavigateToEditVenue: (String) -> Unit,
    onNavigateToReviews: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VenuesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var venueToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    venueToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { venueToDelete = null },
            title = { Text(stringResource(R.string.delete_venue_title)) },
            text = { Text(stringResource(R.string.delete_venue_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVenue(id) { venueToDelete = null }
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { venueToDelete = null }) { Text(stringResource(R.string.no)) }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddVenue) {
                Icon(Icons.Default.Add, contentDescription = "Yangi to'yxona")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadVenues(refresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.venues.isEmpty() -> {
                    EmptyState(
                        message = stringResource(R.string.no_venues_yet),
                        actionLabel = stringResource(R.string.add),
                        onAction = onNavigateToAddVenue
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isOffline) {
                            item { OfflineBanner() }
                        }
                        items(uiState.venues, key = { it.id }) { venue ->
                            VenueOwnerCard(
                                venue = venue,
                                onClick = { onNavigateToVenueDetail(venue.id) }
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { onNavigateToEditVenue(venue.id) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Tahrirlash")
                                }
                                IconButton(onClick = { onNavigateToReviews(venue.id) }) {
                                    Icon(Icons.Default.RateReview, contentDescription = "Sharhlar")
                                }
                                IconButton(
                                    onClick = { venueToDelete = venue.id },
                                    enabled = uiState.deleteInProgress != venue.id
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "O'chirish")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
