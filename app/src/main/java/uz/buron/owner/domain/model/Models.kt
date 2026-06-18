package uz.buron.owner.domain.model

data class User(
    val id: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val telegram: String?,
    val isEnabled: Boolean
) {
    val fullName: String get() = "$firstName $lastName"
}

data class VenueOwner(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val telegram: String?
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class Venue(
    val id: String,
    val owner: VenueOwner,
    val name: String,
    val description: String,
    val address: String,
    val mapLink: String?,
    val location: Location?,
    val region: String,
    val district: String,
    val phone: String,
    val images: List<String>,
    val pricePerSession: Long,
    val capacity: Int,
    val rating: Double,
    val totalBookings: Int,
    val status: String,
    val isEnabled: Boolean
)

data class VenueFormData(
    val name: String,
    val description: String,
    val address: String,
    val mapLink: String? = null,
    val location: Location? = null,
    val region: String,
    val district: String,
    val phone: String,
    val pricePerSession: Long,
    val capacity: Int
)

data class BookingVenueSummary(
    val id: String,
    val name: String,
    val region: String?,
    val district: String?
)

data class Booking(
    val id: String,
    val venue: BookingVenueSummary,
    val clientName: String,
    val clientPhone: String,
    val date: String,
    val sessions: List<String>,
    val status: String,
    val isExternalBooking: Boolean = false,
    val createdAt: String?
)

data class Review(
    val id: String,
    val venue: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val createdAt: String
)

data class CalendarDay(
    val booked: List<String>,
    val available: List<String>,
    val status: String
)

data class CalendarResponse(
    val month: String,
    val calendar: Map<String, CalendarDay>
)

data class BookingsByMonth(
    val month: String,
    val label: String,
    val count: Int
)

data class RevenueByMonth(
    val month: String,
    val label: String,
    val revenue: Long
)

data class StatusBreakdown(
    val name: String,
    val value: Int,
    val status: String
)

data class PerVenueStats(
    val venueId: String,
    val name: String,
    val status: String,
    val bookings: Int,
    val confirmed: Int,
    val revenue: Long
)

data class DashboardStats(
    val totalBookingsThisMonth: Int = 0,
    val revenueEstimate: Long = 0,
    val activeVenues: Int = 0,
    val pendingBookings: Int = 0,
    val totalVenues: Int = 0,
    val recentBookings: List<Booking> = emptyList(),
    val bookingsByMonth: List<BookingsByMonth> = emptyList(),
    val revenueByMonth: List<RevenueByMonth> = emptyList(),
    val statusBreakdown: List<StatusBreakdown> = emptyList(),
    val perVenue: List<PerVenueStats> = emptyList()
)

data class AuthResult(
    val token: String,
    val user: User
)
