package uz.buron.owner.presentation.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.buron.owner.data.api.ApiException
import uz.buron.owner.data.repository.BookingRepository
import uz.buron.owner.data.repository.VenueRepository
import uz.buron.owner.domain.model.Booking
import uz.buron.owner.domain.model.Venue
import javax.inject.Inject

data class BookingsUiState(
    val venues: List<Venue> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val selectedVenueId: String? = null,
    val selectedStatus: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val actionInProgress: String? = null
) {
    val filteredBookings: List<Booking>
        get() {
            var list = bookings
            if (selectedVenueId != null) {
                list = list.filter { it.venue.id == selectedVenueId }
            }
            if (selectedStatus != null) {
                list = list.filter { it.status == selectedStatus }
            }
            return list
        }
}

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private var pollingActive = false

    init {
        loadData()
    }

    fun loadData(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.bookings.isEmpty(),
                    isRefreshing = refresh,
                    error = null
                )
            }
            venueRepository.getMyVenues()
                .onSuccess { venues ->
                    val venueIds = venues.map { it.id }
                    if (venueIds.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                venues = emptyList(),
                                bookings = emptyList(),
                                isLoading = false,
                                isRefreshing = false
                            )
                        }
                        return@launch
                    }
                    bookingRepository.getAllOwnerBookings(venueIds)
                        .onSuccess { bookings ->
                            _uiState.update {
                                it.copy(
                                    venues = venues,
                                    bookings = bookings,
                                    isLoading = false,
                                    isRefreshing = false,
                                    isOffline = false
                                )
                            }
                        }
                        .onFailure { e ->
                            handleError(e, venues)
                        }
                }
                .onFailure { e ->
                    val offline = e is ApiException.Network
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isOffline = offline,
                            error = e.message
                        )
                    }
                }
        }
    }

    private fun handleError(e: Throwable, venues: List<Venue>) {
        val offline = e is ApiException.Network
        _uiState.update {
            it.copy(
                venues = venues,
                isLoading = false,
                isRefreshing = false,
                isOffline = offline,
                error = e.message
            )
        }
    }

    fun setVenueFilter(venueId: String?) {
        _uiState.update { it.copy(selectedVenueId = venueId) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun confirmBooking(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = id) }
            bookingRepository.confirmBooking(id)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            bookings = state.bookings.map { if (it.id == id) updated else it },
                            actionInProgress = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionInProgress = null, error = e.message) }
                }
        }
    }

    fun cancelBooking(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = id) }
            bookingRepository.cancelBooking(id)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            bookings = state.bookings.map { if (it.id == id) updated else it },
                            actionInProgress = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionInProgress = null, error = e.message) }
                }
        }
    }

    fun startPolling() {
        if (pollingActive) return
        pollingActive = true
        viewModelScope.launch {
            while (isActive && pollingActive) {
                delay(60_000)
                loadData(refresh = true)
            }
        }
    }

    fun stopPolling() {
        pollingActive = false
    }
}
