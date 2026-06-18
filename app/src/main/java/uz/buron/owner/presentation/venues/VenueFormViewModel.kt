package uz.buron.owner.presentation.venues

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.BuildConfig
import uz.buron.owner.data.repository.VenueRepository
import uz.buron.owner.domain.model.VenueFormData
import uz.buron.owner.util.PhoneUtils
import uz.buron.owner.util.Validation
import javax.inject.Inject

data class VenueFormUiState(
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val mapLink: String = "",
    val region: String = "",
    val district: String = "",
    val phoneDigits: String = "",
    val pricePerSession: String = "",
    val capacity: String = "",
    val keptImageUrls: List<String> = emptyList(),
    val newImageUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingVenue: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
) {
    val isEdit: Boolean get() = keptImageUrls.isNotEmpty() || newImageUris.isNotEmpty()
    val totalImages: Int get() = keptImageUrls.size + newImageUris.size
}

@HiltViewModel
class VenueFormViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val venueId: String? = savedStateHandle.get<String>("venueId")

    private val _uiState = MutableStateFlow(VenueFormUiState())
    val uiState: StateFlow<VenueFormUiState> = _uiState.asStateFlow()

    init {
        venueId?.let { loadVenue(it) }
    }

    fun loadVenue(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVenue = true) }
            venueRepository.getVenue(id)
                .onSuccess { venue ->
                    val digits = venue.phone.removePrefix("+998").filter { it.isDigit() }.take(9)
                    _uiState.update {
                        it.copy(
                            name = venue.name,
                            description = venue.description,
                            address = venue.address,
                            mapLink = venue.mapLink.orEmpty(),
                            region = venue.region,
                            district = venue.district,
                            phoneDigits = digits,
                            pricePerSession = venue.pricePerSession.toString(),
                            capacity = venue.capacity.toString(),
                            keptImageUrls = venue.images.map { img ->
                                if (img.startsWith("http")) img else "${BuildConfig.UPLOADS_BASE_URL}$img"
                            },
                            isLoadingVenue = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingVenue = false, error = e.message) }
                }
        }
    }

    fun updateName(v: String) = _uiState.update { it.copy(name = v) }
    fun updateDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun updateAddress(v: String) = _uiState.update { it.copy(address = v) }
    fun updateMapLink(v: String) = _uiState.update { it.copy(mapLink = v) }
    fun updateRegion(v: String) = _uiState.update { it.copy(region = v) }
    fun updateDistrict(v: String) = _uiState.update { it.copy(district = v) }
    fun updatePhoneDigits(v: String) = _uiState.update { it.copy(phoneDigits = v) }
    fun updatePrice(v: String) = _uiState.update { it.copy(pricePerSession = v.filter { c -> c.isDigit() }) }
    fun updateCapacity(v: String) = _uiState.update { it.copy(capacity = v.filter { c -> c.isDigit() }) }

    fun addImages(uris: List<Uri>) {
        _uiState.update { it.copy(newImageUris = it.newImageUris + uris) }
    }

    fun removeKeptImage(url: String) {
        _uiState.update { it.copy(keptImageUrls = it.keptImageUrls - url) }
    }

    fun removeNewImage(uri: Uri) {
        _uiState.update { it.copy(newImageUris = it.newImageUris - uri) }
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val errors = mutableMapOf<String, String>()

        Validation.validateVenueName(state.name, "Nomi")?.let { errors["name"] = it }
        if (state.description.trim().length < 10) errors["description"] = "Tavsif kamida 10 belgi"
        if (state.address.trim().length < 5) errors["address"] = "Manzil kamida 5 belgi"
        if (state.region.isBlank()) errors["region"] = "Viloyatni tanlang"
        if (state.district.trim().length < 2) errors["district"] = "Tuman kiriting"
        PhoneUtils.validatePhoneUz("+998${state.phoneDigits}")?.let { errors["phone"] = it }
        val price = state.pricePerSession.toLongOrNull()
        if (price == null || price <= 0) errors["price"] = "Narx noto'g'ri"
        val capacity = state.capacity.toIntOrNull()
        if (capacity == null || capacity < 1) errors["capacity"] = "Sig'im kamida 1"
        val isCreate = venueId == null
        if (isCreate && state.totalImages < 3) {
            errors["images"] = "Kamida 3 ta rasm kerak"
        }
        if (!isCreate && state.totalImages < 1) {
            errors["images"] = "Kamida 1 ta rasm kerak"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }

        val formData = VenueFormData(
            name = state.name.trim(),
            description = state.description.trim(),
            address = state.address.trim(),
            mapLink = state.mapLink.trim().ifBlank { null },
            region = state.region,
            district = state.district.trim(),
            phone = PhoneUtils.normalizePhoneUz("+998${state.phoneDigits}"),
            pricePerSession = price!!,
            capacity = capacity!!
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
            val keptUrls = state.keptImageUrls.map { url ->
                url.removePrefix(BuildConfig.UPLOADS_BASE_URL)
            }
            val result = if (venueId == null) {
                venueRepository.createVenue(formData, state.newImageUris, keptUrls)
            } else {
                venueRepository.updateVenue(venueId, formData, state.newImageUris, keptUrls)
            }
            result
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
