package uz.buron.owner.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.data.repository.BookingRepository
import uz.buron.owner.data.repository.VenueRepository
import uz.buron.owner.domain.model.Venue
import uz.buron.owner.util.DateUtils
import uz.buron.owner.util.PhoneUtils
import uz.buron.owner.util.Validation
import java.time.LocalDate
import javax.inject.Inject

data class ExternalBookingUiState(
    val venues: List<Venue> = emptyList(),
    val selectedVenueId: String? = null,
    val clientName: String = "",
    val phoneDigits: String = "",
    val date: LocalDate? = null,
    val selectedSessions: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingVenues: Boolean = true,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class ExternalBookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExternalBookingUiState())
    val uiState: StateFlow<ExternalBookingUiState> = _uiState.asStateFlow()

    fun initWithVenue(venueId: String?) {
        viewModelScope.launch {
            venueRepository.getMyVenues()
                .onSuccess { venues ->
                    _uiState.update {
                        it.copy(
                            venues = venues,
                            selectedVenueId = venueId ?: venues.firstOrNull()?.id,
                            isLoadingVenues = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingVenues = false, error = e.message) }
                }
        }
    }

    fun selectVenue(id: String) = _uiState.update { it.copy(selectedVenueId = id) }
    fun updateClientName(v: String) = _uiState.update { it.copy(clientName = v) }
    fun updatePhoneDigits(v: String) = _uiState.update { it.copy(phoneDigits = v) }
    fun updateDate(d: LocalDate) = _uiState.update { it.copy(date = d) }

    fun toggleSession(session: String) {
        _uiState.update { state ->
            val sessions = state.selectedSessions.toMutableSet()
            if (sessions.contains(session)) sessions.remove(session) else sessions.add(session)
            state.copy(selectedSessions = sessions)
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val errors = mutableMapOf<String, String>()
        if (state.selectedVenueId.isNullOrBlank()) errors["venue"] = "To'yxonani tanlang"
        Validation.validateName(state.clientName, "Mijoz ismi")?.let { errors["name"] = it }
        PhoneUtils.validatePhoneUz("+998${state.phoneDigits}")?.let { errors["phone"] = it }
        if (state.date == null) errors["date"] = "Sanani tanlang"
        if (state.selectedSessions.isEmpty()) errors["sessions"] = "Kamida bitta sessiya tanlang"

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
            val dateStr = state.date!!.toString()
            bookingRepository.createExternalBooking(
                venueId = state.selectedVenueId!!,
                clientName = state.clientName.trim(),
                clientPhone = PhoneUtils.normalizePhoneUz("+998${state.phoneDigits}"),
                date = dateStr,
                sessions = state.selectedSessions.toList()
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
