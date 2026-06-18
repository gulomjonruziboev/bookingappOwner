package uz.buron.owner.presentation.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.buron.owner.ui.components.FormPhoneField
import uz.buron.owner.ui.components.FormTextField
import uz.buron.owner.ui.components.PrimaryButton
import uz.buron.owner.ui.components.SessionChip
import uz.buron.owner.util.Constants
import uz.buron.owner.util.DateUtils
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExternalBookingSheet(
    venueId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ExternalBookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var venueMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(venueId) {
        viewModel.initWithVenue(venueId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tashqi bron qo'shish", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            val venueLabel = uiState.venues.find { it.id == uiState.selectedVenueId }?.name ?: "Tanlang"
            ExposedDropdownMenuBox(
                expanded = venueMenuExpanded,
                onExpandedChange = { venueMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = venueLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To'yxona") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(venueMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.fieldErrors["venue"] != null,
                    supportingText = uiState.fieldErrors["venue"]?.let { { Text(it) } }
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

            FormTextField(
                value = uiState.clientName,
                onValueChange = viewModel::updateClientName,
                label = "Mijoz ismi",
                error = uiState.fieldErrors["name"]
            )
            FormPhoneField(
                digits = uiState.phoneDigits,
                onDigitsChange = viewModel::updatePhoneDigits,
                label = "Telefon",
                error = uiState.fieldErrors["phone"]
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    uiState.date?.let { DateUtils.formatDisplayDate(it.toString()) }
                        ?: "Sanani tanlang"
                )
            }
            uiState.fieldErrors["date"]?.let {
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }

            Text("Sessiyalar", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Constants.SESSION_ORDER.forEach { session ->
                    val selected = uiState.selectedSessions.contains(session)
                    OutlinedButton(onClick = { viewModel.toggleSession(session) }) {
                        SessionChip(session = session)
                    }
                }
            }
            uiState.fieldErrors["sessions"]?.let {
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }

            uiState.error?.let {
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }

            PrimaryButton(
                text = if (uiState.isLoading) "Saqlanmoqda…" else "Saqlash",
                onClick = { viewModel.submit(onSaved) },
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateDate(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Bekor") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
