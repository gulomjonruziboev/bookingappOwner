package uz.buron.owner.presentation.venues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.data.api.ApiException
import uz.buron.owner.data.repository.VenueRepository
import uz.buron.owner.domain.model.Venue
import javax.inject.Inject

data class VenuesUiState(
    val venues: List<Venue> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val deleteInProgress: String? = null
)

@HiltViewModel
class VenuesViewModel @Inject constructor(
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VenuesUiState())
    val uiState: StateFlow<VenuesUiState> = _uiState.asStateFlow()

    init {
        loadVenues()
    }

    fun loadVenues(refresh: Boolean = false) {
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
                    _uiState.update {
                        it.copy(
                            venues = venues,
                            isLoading = false,
                            isRefreshing = false,
                            isOffline = false,
                            error = null
                        )
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

    fun deleteVenue(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteInProgress = id) }
            venueRepository.deleteVenue(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            venues = state.venues.filter { it.id != id },
                            deleteInProgress = null
                        )
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(deleteInProgress = null, error = e.message) }
                }
        }
    }
}
