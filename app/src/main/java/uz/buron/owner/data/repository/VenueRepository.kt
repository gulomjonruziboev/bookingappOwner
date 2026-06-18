package uz.buron.owner.data.repository

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import uz.buron.owner.data.api.BuronOwnerApiService
import uz.buron.owner.data.api.safeApiCall
import uz.buron.owner.data.dto.toDomain
import uz.buron.owner.domain.model.Venue
import uz.buron.owner.domain.model.VenueFormData
import uz.buron.owner.util.ImageUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenueRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: BuronOwnerApiService,
    private val moshi: Moshi
) {
    suspend fun getMyVenues(): Result<List<Venue>> {
        return safeApiCall(moshi) {
            api.getMyVenues().map { it.toDomain() }
        }
    }

    suspend fun getVenue(id: String): Result<Venue> {
        return safeApiCall(moshi) {
            api.getVenue(id).toDomain()
        }
    }

    suspend fun createVenue(
        fields: VenueFormData,
        newImages: List<Uri>,
        keptImageUrls: List<String> = emptyList()
    ): Result<Venue> {
        return safeApiCall(moshi) {
            val (textParts, imageParts) = buildVenueMultipart(fields, newImages, keptImageUrls)
            api.createVenue(textParts, imageParts).toDomain()
        }
    }

    suspend fun updateVenue(
        id: String,
        fields: VenueFormData,
        newImages: List<Uri>,
        keptImageUrls: List<String>
    ): Result<Venue> {
        return safeApiCall(moshi) {
            val (textParts, imageParts) = buildVenueMultipart(fields, newImages, keptImageUrls)
            api.updateVenue(id, textParts, imageParts).toDomain()
        }
    }

    suspend fun deleteVenue(id: String): Result<String> {
        return safeApiCall(moshi) {
            api.deleteVenue(id).message
        }
    }

    fun buildVenueMultipart(
        fields: VenueFormData,
        newImages: List<Uri>,
        keptImageUrls: List<String> = emptyList()
    ): Pair<Map<String, RequestBody>, List<MultipartBody.Part>> {
        val jsonMediaType = "application/json".toMediaType()
        val textMediaType = "text/plain".toMediaType()
        val imagesJson = moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java)
        ).toJson(keptImageUrls)

        val textParts = mutableMapOf<String, RequestBody>(
            "name" to fields.name.toRequestBody(textMediaType),
            "description" to fields.description.toRequestBody(textMediaType),
            "address" to fields.address.toRequestBody(textMediaType),
            "region" to fields.region.toRequestBody(textMediaType),
            "district" to fields.district.toRequestBody(textMediaType),
            "phone" to fields.phone.toRequestBody(textMediaType),
            "pricePerSession" to fields.pricePerSession.toString().toRequestBody(textMediaType),
            "capacity" to fields.capacity.toString().toRequestBody(textMediaType),
            "images" to imagesJson.toRequestBody(jsonMediaType)
        )

        fields.mapLink?.takeIf { it.isNotBlank() }?.let { link ->
            textParts["mapLink"] = link.toRequestBody(textMediaType)
        }

        fields.location?.let { loc ->
            val locationJson = moshi.adapter(
                uz.buron.owner.data.dto.LocationDto::class.java
            ).toJson(
                uz.buron.owner.data.dto.LocationDto(lat = loc.lat, lng = loc.lng)
            )
            textParts["location"] = locationJson.toRequestBody(jsonMediaType)
        }

        val imageParts = newImages.map { uri ->
            val file = ImageUtils.uriToCompressedFile(context, uri)
            MultipartBody.Part.createFormData(
                "images",
                file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
        }

        return textParts to imageParts
    }

    fun cleanupTempFiles(files: List<File>) {
        files.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
