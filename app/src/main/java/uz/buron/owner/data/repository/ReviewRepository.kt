package uz.buron.owner.data.repository

import com.squareup.moshi.Moshi
import uz.buron.owner.data.api.BuronOwnerApiService
import uz.buron.owner.data.api.safeApiCall
import uz.buron.owner.data.dto.toDomain
import uz.buron.owner.domain.model.Review
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val api: BuronOwnerApiService,
    private val moshi: Moshi
) {
    suspend fun getVenueReviews(venueId: String): Result<List<Review>> {
        return safeApiCall(moshi) {
            api.getVenueReviews(venueId).map { it.toDomain() }
        }
    }
}
