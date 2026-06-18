package uz.buron.owner.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.data.api.ApiException
import uz.buron.owner.data.repository.AuthRepository
import uz.buron.owner.util.PhoneUtils
import uz.buron.owner.util.Validation
import javax.inject.Inject

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val phoneDigits: String = "",
    val telegram: String = "",
    val password: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val phoneError: String? = null,
    val telegramError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, firstNameError = null, error = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, lastNameError = null, error = null) }
    }

    fun onPhoneChange(digits: String) {
        _uiState.update { it.copy(phoneDigits = digits, phoneError = null, error = null) }
    }

    fun onTelegramChange(value: String) {
        _uiState.update { it.copy(telegram = value, telegramError = null, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun register() {
        val state = _uiState.value
        val firstNameError = Validation.validateName(state.firstName, "Ism")
        val lastNameError = Validation.validateName(state.lastName, "Familiya")
        val phone = PhoneUtils.normalizePhoneUz("+998${state.phoneDigits}")
        val phoneError = PhoneUtils.validatePhoneUz(phone)
        val telegram = state.telegram.trim().let {
            if (it.startsWith("@")) it else "@$it"
        }
        val telegramError = Validation.validateTelegram(telegram)
        val passwordError = Validation.validatePassword(state.password)

        if (
            firstNameError != null || lastNameError != null ||
            phoneError != null || telegramError != null || passwordError != null
        ) {
            _uiState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    phoneError = phoneError,
                    telegramError = telegramError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                phone = phone,
                telegram = telegram,
                password = state.password
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false, success = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = mapError(error))
                }
            }
        }
    }

    private fun mapError(error: Throwable): String = when (error) {
        is ApiException.Client -> error.message ?: "Xatolik"
        is ApiException.Network -> "Internet aloqasi yo'q"
        is ApiException.RateLimited -> "Juda ko'p so'rov. Keyinroq urinib ko'ring."
        is ApiException.Server -> "Server xatosi. Keyinroq urinib ko'ring."
        else -> error.message ?: "Xatolik yuz berdi"
    }
}
