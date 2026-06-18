package uz.buron.owner.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import uz.buron.owner.data.dto.AuthResponseDto
import uz.buron.owner.data.dto.BookingDto
import uz.buron.owner.data.dto.CalendarResponseDto
import uz.buron.owner.data.dto.CreateExternalBookingRequestDto
import uz.buron.owner.data.dto.DashboardStatsDto
import uz.buron.owner.data.dto.HealthResponseDto
import uz.buron.owner.data.dto.LoginRequestDto
import uz.buron.owner.data.dto.MessageResponseDto
import uz.buron.owner.data.dto.RegisterOwnerRequestDto
import uz.buron.owner.data.dto.ReviewDto
import uz.buron.owner.data.dto.StatusUpdateRequestDto
import uz.buron.owner.data.dto.UserDto
import uz.buron.owner.data.dto.VenueDto

interface BuronOwnerApiService {

    @GET("health")
    suspend fun health(): HealthResponseDto

    @POST("auth/register")
    suspend fun register(@Body body: RegisterOwnerRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @GET("auth/me")
    suspend fun me(): UserDto

    @GET("venues/owner/my")
    suspend fun getMyVenues(): List<VenueDto>

    @GET("venues/{id}")
    suspend fun getVenue(@Path("id") id: String): VenueDto

    @Multipart
    @POST("venues")
    suspend fun createVenue(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<@JvmSuppressWildcards MultipartBody.Part>
    ): VenueDto

    @Multipart
    @PUT("venues/{id}")
    suspend fun updateVenue(
        @Path("id") id: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<@JvmSuppressWildcards MultipartBody.Part>
    ): VenueDto

    @DELETE("venues/{id}")
    suspend fun deleteVenue(@Path("id") id: String): MessageResponseDto

    @GET("bookings/owner/dashboard")
    suspend fun getDashboard(
        @Query("month") month: String? = null,
        @Query("venueId") venueId: String? = null
    ): DashboardStatsDto

    @Streaming
    @GET("bookings/owner/dashboard/export")
    suspend fun exportDashboard(
        @Query("format") format: String,
        @Query("month") month: String? = null,
        @Query("venueId") venueId: String? = null
    ): ResponseBody

    @GET("bookings/venue/{venueId}")
    suspend fun getVenueBookings(@Path("venueId") venueId: String): List<BookingDto>

    @GET("bookings/venue/{venueId}/calendar")
    suspend fun getCalendar(
        @Path("venueId") venueId: String,
        @Query("month") month: String
    ): CalendarResponseDto

    @POST("bookings")
    suspend fun createBooking(@Body body: CreateExternalBookingRequestDto): BookingDto

    @PUT("bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Path("id") id: String,
        @Body body: StatusUpdateRequestDto
    ): BookingDto

    @GET("reviews/venue/{venueId}")
    suspend fun getVenueReviews(@Path("venueId") venueId: String): List<ReviewDto>
}
