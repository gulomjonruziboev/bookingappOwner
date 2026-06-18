package uz.buron.owner.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.buron.owner.R
import uz.buron.owner.ui.components.LoadingScreen

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadingMessage = stringResource(R.string.loading)
    val errorMessage = uiState.error ?: stringResource(R.string.splash_connection_error)

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            SplashDestination.Main -> onNavigateToMain()
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.None -> Unit
        }
    }

    LoadingScreen(
        message = if (uiState.showRetry) errorMessage else loadingMessage,
        showRetry = uiState.showRetry,
        onRetry = viewModel::startSplash
    )
}
