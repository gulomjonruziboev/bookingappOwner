package uz.buron.owner.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.data.api.ApiException
import uz.buron.owner.data.repository.BookingRepository
import uz.buron.owner.data.repository.VenueRepository
import uz.buron.owner.domain.model.CalendarDay
import uz.buron.owner.domain.model.DashboardStats
import uz.buron.owner.domain.model.Venue
import uz.buron.owner.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class DashboardUiState(
    val venues: List<Venue> = emptyList(),
    val selectedVenueId: String? = null,
    val selectedTab: Int = 0,
    val yearMonth: YearMonth = YearMonth.now(),
    val calendar: Map<String, CalendarDay> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedSessions: Set<String> = emptySet(),
    val stats: DashboardStats? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val isExporting: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.venues.isEmpty(),
                    isRefreshing = refresh,
                    error = null
                )
            }
            venueRepository.getMyVenues()
                .onSuccess { venues ->
                    val venueId = _uiState.value.selectedVenueId ?: venues.firstOrNull()?.id
                    _uiState.update {
                        it.copy(venues = venues, selectedVenueId = venueId)
                    }
                    if (venueId != null) {
                        loadCalendarAndStats(venueId, refresh)
                    } else {
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isOffline = e is ApiException.Network,
                            error = e.message
                        )
                    }
                }
        }
    }

    private suspend fun loadCalendarAndStats(venueId: String, refresh: Boolean) {
        val month = DateUtils.toMonthString(_uiState.value.yearMonth)
        val calendarResult = bookingRepository.getCalendar(venueId, month)
        val statsResult = bookingRepository.getDashboard(month = month, venueId = venueId)

        calendarResult.onSuccess { cal ->
            _uiState.update { it.copy(calendar = cal.calendar) }
        }
        statsResult.onSuccess { stats ->
            _uiState.update {
                it.copy(
                    stats = stats,
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = false
                )
            }
        }

        if (calendarResult.isFailure && statsResult.isFailure) {
            val e = calendarResult.exceptionOrNull() ?: statsResult.exceptionOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = e is ApiException.Network,
                    error = e?.message
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectVenue(venueId: String) {
        _uiState.update { it.copy(selectedVenueId = venueId, selectedDate = null, selectedSessions = emptySet()) }
        viewModelScope.launch { loadCalendarAndStats(venueId, refresh = true) }
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(yearMonth = yearMonth, selectedDate = null, selectedSessions = emptySet()) }
        val venueId = _uiState.value.selectedVenueId ?: return
        viewModelScope.launch { loadCalendarAndStats(venueId, refresh = true) }
    }

    fun onDateSelected(date: LocalDate, availableSessions: List<String>) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedSessions = availableSessions.take(1).toSet()
            )
        }
    }

    fun toggleSession(session: String) {
        _uiState.update { state ->
            val sessions = state.selectedSessions.toMutableSet()
            if (sessions.contains(session)) sessions.remove(session) else sessions.add(session)
            state.copy(selectedSessions = sessions)
        }
    }

    fun exportReport(format: String, onReady: (ByteArray, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val month = DateUtils.toMonthString(_uiState.value.yearMonth)
            bookingRepository.exportDashboard(format, month, _uiState.value.selectedVenueId)
                .onSuccess { (bytes, filename) ->
                    _uiState.update { it.copy(isExporting = false) }
                    onReady(bytes, filename)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExporting = false, error = e.message) }
                }
        }
    }
}
