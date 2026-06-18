package uz.buron.owner.presentation.bookings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import uz.buron.owner.ui.components.BookingCard
import uz.buron.owner.ui.components.EmptyState
import uz.buron.owner.ui.components.OfflineBanner
import uz.buron.owner.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onNavigateToExternalBooking: (venueId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var venueMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val selectedVenueLabel = uiState.selectedVenueId?.let { id ->
        uiState.venues.find { it.id == id }?.name
    } ?: stringResource(R.string.all_venues)

    val selectedStatusLabel = uiState.selectedStatus?.let {
        Constants.STATUS_LABELS[it]
    } ?: stringResource(R.string.all_statuses)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val venueId = uiState.selectedVenueId ?: uiState.venues.firstOrNull()?.id
                onNavigateToExternalBooking(venueId)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tashqi bron")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadData(refresh = true) },
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
                    EmptyState(message = stringResource(R.string.add_venue_first))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isOffline) {
                            item { OfflineBanner() }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = venueMenuExpanded,
                                    onExpandedChange = { venueMenuExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedVenueLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.field_venue)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(venueMenuExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = venueMenuExpanded,
                                        onDismissRequest = { venueMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.all_venues)) },
                                            onClick = {
                                                viewModel.setVenueFilter(null)
                                                venueMenuExpanded = false
                                            }
                                        )
                                        uiState.venues.forEach { venue ->
                                            DropdownMenuItem(
                                                text = { Text(venue.name) },
                                                onClick = {
                                                    viewModel.setVenueFilter(venue.id)
                                                    venueMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = statusMenuExpanded,
                                    onExpandedChange = { statusMenuExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedStatusLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.field_status)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(statusMenuExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = statusMenuExpanded,
                                        onDismissRequest = { statusMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.all_statuses)) },
                                            onClick = {
                                                viewModel.setStatusFilter(null)
                                                statusMenuExpanded = false
                                            }
                                        )
                                        Constants.STATUS_LABELS.forEach { (key, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    viewModel.setStatusFilter(key)
                                                    statusMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.filteredBookings.isEmpty()) {
                            item {
                                EmptyState(message = stringResource(R.string.no_bookings))
                            }
                        } else {
                            items(uiState.filteredBookings, key = { it.id }) { booking ->
                                BookingCard(
                                    booking = booking,
                                    onConfirm = if (booking.status == "pending") {
                                        { viewModel.confirmBooking(booking.id) }
                                    } else null,
                                    onCancel = if (booking.status == "pending") {
                                        { viewModel.cancelBooking(booking.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
