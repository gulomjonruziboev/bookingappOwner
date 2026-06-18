package uz.buron.owner.presentation.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.buron.owner.R
import uz.buron.owner.domain.model.BookingsByMonth
import uz.buron.owner.domain.model.PerVenueStats
import uz.buron.owner.domain.model.RevenueByMonth
import uz.buron.owner.domain.model.StatusBreakdown
import uz.buron.owner.ui.components.AvailabilityCalendar
import uz.buron.owner.ui.components.EmptyState
import uz.buron.owner.ui.components.OfflineBanner
import uz.buron.owner.ui.components.PrimaryButton
import uz.buron.owner.ui.components.StatusBadge
import uz.buron.owner.ui.theme.ErrorRed
import uz.buron.owner.ui.theme.PurplePrimary
import uz.buron.owner.ui.theme.SuccessGreen
import uz.buron.owner.ui.theme.WarningYellow
import uz.buron.owner.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToExternalBooking: (venueId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var venueMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val pending = pendingExport
        if (uri != null && pending != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(pending.first)
            }
        }
        pendingExport = null
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val selectedVenueLabel = uiState.venues.find { it.id == uiState.selectedVenueId }?.name
        ?: stringResource(R.string.select_venue)

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.loadInitial(refresh = true) },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.venues.isEmpty() -> {
                EmptyState(message = stringResource(R.string.add_stats_venue_first))
            }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    if (uiState.isOffline) OfflineBanner()

                    ExposedDropdownMenuBox(
                        expanded = venueMenuExpanded,
                        onExpandedChange = { venueMenuExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedVenueLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.field_venue)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(venueMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = venueMenuExpanded,
                            onDismissRequest = { venueMenuExpanded = false }
                        ) {
                            uiState.venues.forEach { venue ->
                                DropdownMenuItem(
                                    text = { Text(venue.name) },
                                    onClick = {
                                        viewModel.selectVenue(venue.id)
                                        venueMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            text = { Text(stringResource(R.string.tab_calendar)) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = { Text(stringResource(R.string.tab_statistics)) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (uiState.selectedTab) {
                            0 -> CalendarTab(
                            uiState = uiState,
                            onMonthChange = viewModel::changeMonth,
                            onDateSelected = viewModel::onDateSelected,
                            onSessionToggle = viewModel::toggleSession,
                            onAddExternalBooking = {
                                val venueId = uiState.selectedVenueId
                                if (venueId != null && uiState.selectedDate != null &&
                                    uiState.selectedSessions.isNotEmpty()
                                ) {
                                    onNavigateToExternalBooking(venueId)
                                } else {
                                    onNavigateToExternalBooking(uiState.selectedVenueId)
                                }
                            }
                        )
                        1 -> StatisticsTab(
                            stats = uiState.stats,
                            isExporting = uiState.isExporting,
                            onExportExcel = {
                                viewModel.exportReport("xlsx") { bytes, name ->
                                    pendingExport = bytes to name
                                    exportLauncher.launch(name)
                                }
                            },
                            onExportPdf = {
                                viewModel.exportReport("pdf") { bytes, name ->
                                    pendingExport = bytes to name
                                    exportLauncher.launch(name)
                                }
                            }
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTab(
    uiState: DashboardUiState,
    onMonthChange: (java.time.YearMonth) -> Unit,
    onDateSelected: (java.time.LocalDate, List<String>) -> Unit,
    onSessionToggle: (String) -> Unit,
    onAddExternalBooking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvailabilityCalendar(
            yearMonth = uiState.yearMonth,
            calendar = uiState.calendar,
            selectedDate = uiState.selectedDate,
            selectedSessions = uiState.selectedSessions,
            onMonthChange = onMonthChange,
            onDateSelected = onDateSelected,
            onSessionToggle = onSessionToggle
        )
        PrimaryButton(
            text = stringResource(R.string.add_external_booking),
            onClick = onAddExternalBooking,
            enabled = uiState.selectedVenueId != null
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatisticsTab(
    stats: uz.buron.owner.domain.model.DashboardStats?,
    isExporting: Boolean,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit
) {
    if (stats == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(stringResource(R.string.stat_bookings_month), stats.totalBookingsThisMonth.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_revenue), PriceFormatter.format(stats.revenueEstimate), Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(stringResource(R.string.stat_active_venues), stats.activeVenues.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_pending), stats.pendingBookings.toString(), Modifier.weight(1f))
            }
        }

        item {
            Text(stringResource(R.string.chart_bookings_by_month), style = MaterialTheme.typography.titleMedium)
            SimpleBarChart(
                items = stats.bookingsByMonth.map { it.label to it.count.toFloat() },
                barColor = PurplePrimary
            )
        }

        item {
            Text(stringResource(R.string.chart_revenue_by_month), style = MaterialTheme.typography.titleMedium)
            SimpleBarChart(
                items = stats.revenueByMonth.map { it.label to it.revenue.toFloat() },
                barColor = SuccessGreen,
                formatValue = { PriceFormatter.format(it.toLong()) }
            )
        }

        item {
            Text(stringResource(R.string.chart_by_status), style = MaterialTheme.typography.titleMedium)
            stats.statusBreakdown.forEach { item ->
                StatusBreakdownRow(item)
            }
        }

        item {
            Text(stringResource(R.string.chart_by_venue), style = MaterialTheme.typography.titleMedium)
            stats.perVenue.forEach { venue ->
                PerVenueRow(venue)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onExportExcel,
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.export_excel)) }
                OutlinedButton(
                    onClick = onExportPdf,
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.export_pdf)) }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SimpleBarChart(
    items: List<Pair<String, Float>>,
    barColor: Color,
    formatValue: (Float) -> String = { it.toInt().toString() }
) {
    val max = items.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, modifier = Modifier.weight(0.35f), style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .height(20.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value / max)
                            .height(20.dp)
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }
                Text(formatValue(value), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusBreakdownRow(item: StatusBreakdown) {
    val color = when (item.status) {
        "pending" -> WarningYellow
        "confirmed" -> SuccessGreen
        "cancelled" -> ErrorRed
        else -> PurplePrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.05f)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(item.name)
        }
        Text(item.value.toString(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PerVenueRow(venue: PerVenueStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(venue.name, fontWeight = FontWeight.SemiBold)
                StatusBadge(status = venue.status, isVenueStatus = true)
            }
            Text(stringResource(R.string.venue_bookings_confirmed, venue.bookings, venue.confirmed))
            Text(stringResource(R.string.venue_revenue, PriceFormatter.format(venue.revenue)))
        }
    }
}
