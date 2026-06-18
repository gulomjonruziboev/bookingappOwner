package uz.buron.owner.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorDto(val message: String? = null)

@JsonClass(generateAdapter = true)
data class HealthResponseDto(val status: String)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val phone: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterOwnerRequestDto(
    val role: String = "owner",
    val firstName: String,
    val lastName: String,
    val phone: String,
    val telegram: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "_id") val id: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val telegram: String? = null,
    val isEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class VenueOwnerDto(
    @Json(name = "_id") val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val telegram: String? = null
)

@JsonClass(generateAdapter = true)
data class LocationDto(val lat: Double, val lng: Double)

@JsonClass(generateAdapter = true)
data class VenueDto(
    @Json(name = "_id") val id: String,
    val owner: VenueOwnerDto? = null,
    val name: String,
    val description: String = "",
    val address: String = "",
    val mapLink: String? = null,
    val location: LocationDto? = null,
    val region: String,
    val district: String,
    val phone: String,
    val images: List<String> = emptyList(),
    val pricePerSession: Long,
    val capacity: Int = 0,
    val rating: Double = 0.0,
    val totalBookings: Int = 0,
    val status: String = "pending",
    val isEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class BookingVenueSummaryDto(
    @Json(name = "_id") val id: String? = null,
    val name: String,
    val region: String? = null,
    val district: String? = null
)

@JsonClass(generateAdapter = true)
data class BookingDto(
    @Json(name = "_id") val id: String,
    val venue: BookingVenueSummaryDto? = null,
    val clientName: String,
    val clientPhone: String,
    val date: String,
    val sessions: List<String>,
    val status: String,
    val isExternalBooking: Boolean = false,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ReviewDto(
    @Json(name = "_id") val id: String,
    val venue: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CalendarDayDto(
    val booked: List<String> = emptyList(),
    val available: List<String> = emptyList(),
    val status: String
)

@JsonClass(generateAdapter = true)
data class CalendarResponseDto(
    val month: String,
    val calendar: Map<String, CalendarDayDto>
)

@JsonClass(generateAdapter = true)
data class BookingsByMonthDto(
    val month: String,
    val label: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class RevenueByMonthDto(
    val month: String,
    val label: String,
    val revenue: Long
)

@JsonClass(generateAdapter = true)
data class StatusBreakdownDto(
    val name: String,
    val value: Int,
    val status: String
)

@JsonClass(generateAdapter = true)
data class PerVenueStatsDto(
    val venueId: String,
    val name: String,
    val status: String,
    val bookings: Int,
    val confirmed: Int,
    val revenue: Long
)

@JsonClass(generateAdapter = true)
data class DashboardStatsDto(
    val totalBookingsThisMonth: Int = 0,
    val revenueEstimate: Long = 0,
    val activeVenues: Int = 0,
    val pendingBookings: Int = 0,
    val totalVenues: Int = 0,
    val recentBookings: List<BookingDto> = emptyList(),
    val bookingsByMonth: List<BookingsByMonthDto> = emptyList(),
    val revenueByMonth: List<RevenueByMonthDto> = emptyList(),
    val statusBreakdown: List<StatusBreakdownDto> = emptyList(),
    val perVenue: List<PerVenueStatsDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StatusUpdateRequestDto(val status: String)

@JsonClass(generateAdapter = true)
data class CreateExternalBookingRequestDto(
    val venueId: String,
    val clientName: String,
    val clientPhone: String,
    val date: String,
    val sessions: List<String>,
    val isExternalBooking: Boolean = true
)

@JsonClass(generateAdapter = true)
data class MessageResponseDto(val message: String)
