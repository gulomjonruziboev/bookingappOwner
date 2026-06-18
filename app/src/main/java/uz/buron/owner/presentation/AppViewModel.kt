package uz.buron.owner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.buron.owner.data.local.TokenStore
import uz.buron.owner.data.repository.AuthRepository
import uz.buron.owner.data.repository.BookingRepository
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    val authState: StateFlow<TokenStore.AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TokenStore.AuthState(null, false))

    val pendingBookingsCount: StateFlow<Int> = bookingRepository.pendingBookingsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        refreshPendingCount()
    }

    fun refreshPendingCount() {
        if (!authRepository.authState.value.isLoggedIn) return
        viewModelScope.launch {
            bookingRepository.refreshPendingCount()
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
