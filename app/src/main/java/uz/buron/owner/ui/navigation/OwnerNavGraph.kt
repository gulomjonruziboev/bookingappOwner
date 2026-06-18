package uz.buron.owner.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.buron.owner.presentation.AppViewModel
import uz.buron.owner.presentation.MainShell
import uz.buron.owner.presentation.MainTab
import uz.buron.owner.presentation.auth.LoginScreen
import uz.buron.owner.presentation.auth.RegisterScreen
import uz.buron.owner.presentation.auth.SplashScreen
import uz.buron.owner.presentation.booking.ExternalBookingSheet
import uz.buron.owner.presentation.bookings.BookingsScreen
import uz.buron.owner.presentation.dashboard.DashboardScreen
import uz.buron.owner.presentation.profile.ProfileScreen
import uz.buron.owner.presentation.reviews.ReviewsScreen
import uz.buron.owner.presentation.venues.VenueDetailScreen
import uz.buron.owner.presentation.venues.VenueFormScreen
import uz.buron.owner.presentation.venues.VenuesScreen
import uz.buron.owner.presentation.venues.VenuesViewModel

object OwnerRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val BOOKINGS = "bookings"
    const val VENUES = "venues"
    const val PROFILE = "profile"
    const val ADD_VENUE = "add_venue"
    const val EDIT_VENUE = "edit_venue/{venueId}"
    const val VENUE_DETAIL = "venue_detail/{venueId}"
    const val REVIEWS = "reviews/{venueId}"

    val bottomNavRoutes = setOf(DASHBOARD, BOOKINGS, VENUES, PROFILE)

    fun editVenue(venueId: String) = "edit_venue/$venueId"
    fun venueDetail(venueId: String) = "venue_detail/$venueId"
    fun reviews(venueId: String) = "reviews/$venueId"
}

@Composable
fun OwnerApp(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var externalBookingVenueId by remember { mutableStateOf<String?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("/{")
    val showMainShell = currentRoute in OwnerRoutes.bottomNavRoutes

    val currentTab = when (currentRoute) {
        OwnerRoutes.BOOKINGS -> MainTab.Bookings
        OwnerRoutes.VENUES -> MainTab.Venues
        OwnerRoutes.PROFILE -> MainTab.Profile
        else -> MainTab.Dashboard
    }

    val navContent: @Composable (Modifier) -> Unit = { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = OwnerRoutes.SPLASH,
            modifier = contentModifier
        ) {
            composable(OwnerRoutes.SPLASH) {
                SplashScreen(
                    onNavigateToMain = {
                        navController.navigate(OwnerRoutes.DASHBOARD) {
                            popUpTo(OwnerRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(OwnerRoutes.LOGIN) {
                            popUpTo(OwnerRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(OwnerRoutes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        appViewModel.refreshPendingCount()
                        navController.navigate(OwnerRoutes.DASHBOARD) {
                            popUpTo(OwnerRoutes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(OwnerRoutes.REGISTER)
                    }
                )
            }

            composable(OwnerRoutes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        appViewModel.refreshPendingCount()
                        navController.navigate(OwnerRoutes.DASHBOARD) {
                            popUpTo(OwnerRoutes.REGISTER) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(OwnerRoutes.LOGIN) {
                            popUpTo(OwnerRoutes.REGISTER) { inclusive = true }
                        }
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            composable(OwnerRoutes.DASHBOARD) {
                DashboardScreen(onNavigateToExternalBooking = { externalBookingVenueId = it })
            }

            composable(OwnerRoutes.BOOKINGS) {
                BookingsScreen(onNavigateToExternalBooking = { externalBookingVenueId = it })
            }

            composable(OwnerRoutes.VENUES) {
                VenuesScreen(
                    onNavigateToAddVenue = {
                        navController.navigate(OwnerRoutes.ADD_VENUE)
                    },
                    onNavigateToVenueDetail = { id ->
                        navController.navigate(OwnerRoutes.venueDetail(id))
                    },
                    onNavigateToEditVenue = { id ->
                        navController.navigate(OwnerRoutes.editVenue(id))
                    },
                    onNavigateToReviews = { id ->
                        navController.navigate(OwnerRoutes.reviews(id))
                    }
                )
            }

            composable(OwnerRoutes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        appViewModel.logout()
                        navController.navigate(OwnerRoutes.LOGIN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(OwnerRoutes.ADD_VENUE) { backStackEntry ->
                val venuesEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(OwnerRoutes.VENUES)
                }
                val venuesViewModel: VenuesViewModel = hiltViewModel(venuesEntry)
                VenueFormScreen(
                    venueId = null,
                    onSaved = {
                        venuesViewModel.loadVenues(refresh = true)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = OwnerRoutes.EDIT_VENUE,
                arguments = listOf(navArgument("venueId") { type = NavType.StringType })
            ) { entry ->
                val venueId = entry.arguments?.getString("venueId") ?: return@composable
                val venuesEntry = remember(entry) {
                    navController.getBackStackEntry(OwnerRoutes.VENUES)
                }
                val venuesViewModel: VenuesViewModel = hiltViewModel(venuesEntry)
                VenueFormScreen(
                    venueId = venueId,
                    onSaved = {
                        venuesViewModel.loadVenues(refresh = true)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = OwnerRoutes.VENUE_DETAIL,
                arguments = listOf(navArgument("venueId") { type = NavType.StringType })
            ) { entry ->
                val venueId = entry.arguments?.getString("venueId") ?: return@composable
                VenueDetailScreen(
                    venueId = venueId,
                    onNavigateToEdit = { id ->
                        navController.navigate(OwnerRoutes.editVenue(id))
                    },
                    onNavigateToReviews = { id ->
                        navController.navigate(OwnerRoutes.reviews(id))
                    },
                    onNavigateToExternalBooking = { id ->
                        externalBookingVenueId = id
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = OwnerRoutes.REVIEWS,
                arguments = listOf(navArgument("venueId") { type = NavType.StringType })
            ) { entry ->
                val venueId = entry.arguments?.getString("venueId") ?: return@composable
                ReviewsScreen(
                    venueId = venueId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showMainShell) {
        MainShell(
            currentTab = currentTab,
            onTabSelected = { tab ->
                navController.navigate(tab.route) {
                    popUpTo(OwnerRoutes.DASHBOARD) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            snackbarHostState = snackbarHostState,
            content = navContent
        )
    } else {
        navContent(Modifier)
    }

    externalBookingVenueId?.let { venueId ->
        ExternalBookingSheet(
            venueId = venueId,
            onDismiss = { externalBookingVenueId = null },
            onSaved = {
                externalBookingVenueId = null
                appViewModel.refreshPendingCount()
            }
        )
    }
}
