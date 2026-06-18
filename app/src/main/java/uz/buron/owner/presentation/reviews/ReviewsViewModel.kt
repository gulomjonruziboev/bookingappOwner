package uz.buron.owner.presentation.reviews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.data.api.ApiException
import uz.buron.owner.data.repository.ReviewRepository
import uz.buron.owner.domain.model.Review
import javax.inject.Inject

data class ReviewsUiState(
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val venueId: String = checkNotNull(savedStateHandle["venueId"])

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    init {
        loadReviews()
    }

    fun loadReviews(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.reviews.isEmpty(),
                    isRefreshing = refresh,
                    error = null
                )
            }
            reviewRepository.getVenueReviews(venueId)
                .onSuccess { reviews ->
                    _uiState.update {
                        it.copy(
                            reviews = reviews,
                            isLoading = false,
                            isRefreshing = false,
                            isOffline = false
                        )
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
}
