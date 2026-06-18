package uz.buron.owner.data.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import uz.buron.owner.domain.model.AuthResult
import uz.buron.owner.domain.model.Booking
import uz.buron.owner.domain.model.BookingVenueSummary
import uz.buron.owner.domain.model.BookingsByMonth
import uz.buron.owner.domain.model.CalendarDay
import uz.buron.owner.domain.model.CalendarResponse
import uz.buron.owner.domain.model.DashboardStats
import uz.buron.owner.domain.model.Location
import uz.buron.owner.domain.model.PerVenueStats
import uz.buron.owner.domain.model.RevenueByMonth
import uz.buron.owner.domain.model.Review
import uz.buron.owner.domain.model.StatusBreakdown
import uz.buron.owner.domain.model.User
import uz.buron.owner.domain.model.Venue
import uz.buron.owner.domain.model.VenueOwner

private val defaultVenueOwner = VenueOwner(
    id = "",
    firstName = "",
    lastName = "",
    phone = null,
    telegram = null
)

fun UserDto.toDomain(): User = User(
    id = id,
    role = role,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    telegram = telegram,
    isEnabled = isEnabled
)

fun AuthResponseDto.toDomain(): AuthResult = AuthResult(
    token = token,
    user = user.toDomain()
)

fun VenueOwnerDto.toDomain(): VenueOwner = VenueOwner(
    id = id,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    telegram = telegram
)

fun LocationDto.toDomain(): Location = Location(lat = lat, lng = lng)

fun VenueDto.toDomain(): Venue = Venue(
    id = id,
    owner = owner?.toDomain() ?: defaultVenueOwner,
    name = name,
    description = description,
    address = address,
    mapLink = mapLink,
    location = location?.toDomain(),
    region = region,
    district = district,
    phone = phone,
    images = images,
    pricePerSession = pricePerSession,
    capacity = capacity,
    rating = rating,
    totalBookings = totalBookings,
    status = status,
    isEnabled = isEnabled
)

fun BookingVenueSummaryDto.toDomain(): BookingVenueSummary = BookingVenueSummary(
    id = id.orEmpty(),
    name = name,
    region = region,
    district = district
)

fun BookingDto.toDomain(): Booking {
    val venueSummary = venue?.toDomain()
        ?: BookingVenueSummary(id = "", name = "Noma'lum", region = null, district = null)

    return Booking(
        id = id,
        venue = venueSummary,
        clientName = clientName,
        clientPhone = clientPhone,
        date = date,
        sessions = sessions,
        status = status,
        isExternalBooking = isExternalBooking,
        createdAt = createdAt
    )
}

fun ReviewDto.toDomain(): Review = Review(
    id = id,
    venue = venue,
    authorName = authorName,
    rating = rating,
    comment = comment,
    createdAt = createdAt
)

fun CalendarDayDto.toDomain(): CalendarDay = CalendarDay(
    booked = booked,
    available = available,
    status = status
)

fun CalendarResponseDto.toDomain(): CalendarResponse = CalendarResponse(
    month = month,
    calendar = calendar.mapValues { it.value.toDomain() }
)

fun BookingsByMonthDto.toDomain(): BookingsByMonth = BookingsByMonth(
    month = month,
    label = label,
    count = count
)

fun RevenueByMonthDto.toDomain(): RevenueByMonth = RevenueByMonth(
    month = month,
    label = label,
    revenue = revenue
)

fun StatusBreakdownDto.toDomain(): StatusBreakdown = StatusBreakdown(
    name = name,
    value = value,
    status = status
)

fun PerVenueStatsDto.toDomain(): PerVenueStats = PerVenueStats(
    venueId = venueId,
    name = name,
    status = status,
    bookings = bookings,
    confirmed = confirmed,
    revenue = revenue
)

fun DashboardStatsDto.toDomain(): DashboardStats = DashboardStats(
    totalBookingsThisMonth = totalBookingsThisMonth,
    revenueEstimate = revenueEstimate,
    activeVenues = activeVenues,
    pendingBookings = pendingBookings,
    totalVenues = totalVenues,
    recentBookings = recentBookings.map { it.toDomain() },
    bookingsByMonth = bookingsByMonth.map { it.toDomain() },
    revenueByMonth = revenueByMonth.map { it.toDomain() },
    statusBreakdown = statusBreakdown.map { it.toDomain() },
    perVenue = perVenue.map { it.toDomain() }
)

fun createMoshi(): Moshi = Moshi.Builder()
    .add(BookingVenueRefAdapter())
    .add(KotlinJsonAdapterFactory())
    .build()
