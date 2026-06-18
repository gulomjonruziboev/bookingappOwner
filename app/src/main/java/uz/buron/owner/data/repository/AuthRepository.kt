package uz.buron.owner.data.repository

import com.squareup.moshi.Moshi
import uz.buron.owner.data.api.ApiException
import uz.buron.owner.data.api.BuronOwnerApiService
import uz.buron.owner.data.api.safeApiCall
import uz.buron.owner.data.dto.LoginRequestDto
import uz.buron.owner.data.dto.RegisterOwnerRequestDto
import uz.buron.owner.data.dto.toDomain
import uz.buron.owner.data.local.TokenStore
import uz.buron.owner.data.local.UserPreferences
import uz.buron.owner.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: BuronOwnerApiService,
    private val tokenStore: TokenStore,
    private val userPreferences: UserPreferences,
    private val moshi: Moshi
) {
    val authState = tokenStore.authState

    suspend fun pingHealth(): Result<Unit> {
        return safeApiCall(moshi) {
            val response = api.health()
            if (response.status != "ok") {
                throw ApiException.Server("Server javob bermadi")
            }
        }
    }

    suspend fun validateSession(): Result<User?> {
        if (!tokenStore.isLoggedIn()) return Result.success(null)
        return safeApiCall(moshi) {
            val user = api.me().toDomain()
            if (user.role != OWNER_ROLE) {
                tokenStore.clear()
                throw ApiException.Client(OWNER_ONLY_MESSAGE, 403)
            }
            tokenStore.saveSession(tokenStore.getToken()!!, user)
            user
        }
    }

    suspend fun login(phone: String, password: String, rememberMe: Boolean): Result<User> {
        return safeApiCall(moshi) {
            val response = api.login(LoginRequestDto(phone = phone, password = password))
            if (response.user.role != OWNER_ROLE) {
                throw ApiException.Client(OWNER_ONLY_MESSAGE, 403)
            }
            val auth = response.toDomain()
            tokenStore.saveSession(auth.token, auth.user)
            userPreferences.setRememberMe(rememberMe)
            if (rememberMe) {
                userPreferences.savePhoneDigits(phone.removePrefix("+998"))
            } else {
                userPreferences.clearSavedCredentials()
            }
            auth.user
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        phone: String,
        telegram: String,
        password: String
    ): Result<User> {
        return safeApiCall(moshi) {
            val response = api.register(
                RegisterOwnerRequestDto(
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    telegram = telegram,
                    password = password
                )
            )
            if (response.user.role != OWNER_ROLE) {
                throw ApiException.Client(OWNER_ONLY_MESSAGE, 403)
            }
            val auth = response.toDomain()
            tokenStore.saveSession(auth.token, auth.user)
            auth.user
        }
    }

    fun logout() {
        tokenStore.clear()
    }

    fun getCurrentUser(): User? = tokenStore.getUser()

    suspend fun getMe(): Result<User> {
        return safeApiCall(moshi) {
            val user = api.me().toDomain()
            if (user.role != OWNER_ROLE) {
                tokenStore.clear()
                throw ApiException.Client(OWNER_ONLY_MESSAGE, 403)
            }
            tokenStore.saveSession(tokenStore.getToken()!!, user)
            user
        }
    }

    fun getRememberMePhoneDigits(): String? {
        return if (userPreferences.isRememberMeEnabled()) {
            userPreferences.getSavedPhoneDigits().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    fun isRememberMeEnabled(): Boolean = userPreferences.isRememberMeEnabled()

    companion object {
        const val OWNER_ROLE = "owner"
        const val OWNER_ONLY_MESSAGE = "Bu ilova faqat to'yxona egalari uchun"
    }
}
