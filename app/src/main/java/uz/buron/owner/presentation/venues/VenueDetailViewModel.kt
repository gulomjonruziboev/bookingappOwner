package uz.buron.owner.presentation.venues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.data.repository.VenueRepository
import uz.buron.owner.domain.model.Venue
import javax.inject.Inject

data class VenueDetailUiState(
    val venue: Venue? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class VenueDetailViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val venueId: String = checkNotNull(savedStateHandle["venueId"])

    private val _uiState = MutableStateFlow(VenueDetailUiState())
    val uiState: StateFlow<VenueDetailUiState> = _uiState.asStateFlow()

    init {
        loadVenue()
    }

    fun loadVenue() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            venueRepository.getVenue(venueId)
                .onSuccess { venue ->
                    _uiState.update { it.copy(venue = venue, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
