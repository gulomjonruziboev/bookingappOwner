package uz.buron.owner.data.repository

import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.buron.owner.data.api.BuronOwnerApiService
import uz.buron.owner.data.api.safeApiCall
import uz.buron.owner.data.dto.StatusUpdateRequestDto
import uz.buron.owner.data.dto.toDomain
import uz.buron.owner.domain.model.Booking
import uz.buron.owner.domain.model.DashboardStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val api: BuronOwnerApiService,
    private val moshi: Moshi
) {
    private val _pendingBookingsCount = MutableStateFlow(0)
    val pendingBookingsCount: StateFlow<Int> = _pendingBookingsCount.asStateFlow()

    suspend fun refreshPendingCount(): Result<Int> {
        return safeApiCall(moshi) {
            val stats = api.getDashboard().toDomain()
            _pendingBookingsCount.value = stats.pendingBookings
            stats.pendingBookings
        }
    }

    suspend fun getDashboard(month: String? = null, venueId: String? = null): Result<DashboardStats> {
        return safeApiCall(moshi) {
            val stats = api.getDashboard(month = month, venueId = venueId).toDomain()
            _pendingBookingsCount.value = stats.pendingBookings
            stats
        }
    }

    suspend fun getVenueBookings(venueId: String): Result<List<Booking>> {
        return safeApiCall(moshi) {
            api.getVenueBookings(venueId).map { it.toDomain() }
        }
    }

    suspend fun confirmBooking(id: String): Result<Booking> = updateStatus(id, "confirmed")

    suspend fun cancelBooking(id: String): Result<Booking> = updateStatus(id, "cancelled")

    suspend fun getCalendar(venueId: String, month: String): Result<uz.buron.owner.domain.model.CalendarResponse> {
        return safeApiCall(moshi) {
            api.getCalendar(venueId, month).toDomain()
        }
    }

    suspend fun createExternalBooking(
        venueId: String,
        clientName: String,
        clientPhone: String,
        date: String,
        sessions: List<String>
    ): Result<Booking> {
        return safeApiCall(moshi) {
            api.createBooking(
                uz.buron.owner.data.dto.CreateExternalBookingRequestDto(
                    venueId = venueId,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    date = date,
                    sessions = sessions,
                    isExternalBooking = true
                )
            ).toDomain()
        }.also { result ->
            if (result.isSuccess) refreshPendingCount()
        }
    }

    suspend fun getAllOwnerBookings(venueIds: List<String>): Result<List<Booking>> {
        return safeApiCall(moshi) {
            venueIds.flatMap { venueId ->
                api.getVenueBookings(venueId).map { it.toDomain() }
            }.sortedByDescending { it.date }
        }
    }

    suspend fun exportDashboard(
        format: String,
        month: String? = null,
        venueId: String? = null
    ): Result<Pair<ByteArray, String>> {
        return safeApiCall(moshi) {
            val body = api.exportDashboard(format = format, month = month, venueId = venueId)
            val bytes = body.bytes()
            val extension = if (format == "pdf") "pdf" else "xlsx"
            val mime = if (format == "pdf") "application/pdf" else
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            bytes to "buron-report-$month.$extension"
        }
    }

    private suspend fun updateStatus(id: String, status: String): Result<Booking> {
        return safeApiCall(moshi) {
            api.updateBookingStatus(id, StatusUpdateRequestDto(status)).toDomain()
        }.also { result ->
            if (result.isSuccess) refreshPendingCount()
        }
    }
}
