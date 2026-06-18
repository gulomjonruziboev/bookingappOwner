package uz.buron.owner.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buron.owner.R
import uz.buron.owner.data.repository.AuthRepository
import javax.inject.Inject

enum class SplashDestination {
    None,
    Main,
    Login
}

data class SplashUiState(
    val isLoading: Boolean = true,
    val showRetry: Boolean = false,
    val error: String? = null,
    val destination: SplashDestination = SplashDestination.None
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        startSplash()
    }

    fun startSplash() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    showRetry = false,
                    error = null,
                    destination = SplashDestination.None
                )
            }

            val healthOk = pingHealthWithRetry()
            if (!healthOk) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showRetry = true,
                        error = context.getString(R.string.splash_connection_hint)
                    )
                }
                return@launch
            }

            val destination = if (authRepository.authState.value.isLoggedIn) {
                authRepository.validateSession()
                    .fold(
                        onSuccess = { user ->
                            if (user != null) SplashDestination.Main else SplashDestination.Login
                        },
                        onFailure = { SplashDestination.Login }
                    )
            } else {
                SplashDestination.Login
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    destination = destination
                )
            }
        }
    }

    private suspend fun pingHealthWithRetry(maxAttempts: Int = 3): Boolean {
        repeat(maxAttempts) { attempt ->
            val result = authRepository.pingHealth()
            if (result.isSuccess) return true
            if (attempt < maxAttempts - 1) {
                delay(3_000L * (attempt + 1))
            }
        }
        return false
    }
}
