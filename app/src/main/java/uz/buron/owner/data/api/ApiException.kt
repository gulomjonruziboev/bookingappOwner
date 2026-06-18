package uz.buron.owner.data.api

import com.squareup.moshi.Moshi
import retrofit2.HttpException
import uz.buron.owner.data.dto.ApiErrorDto
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class ApiException(message: String, val statusCode: Int? = null) : Exception(message) {
    class Network(message: String) : ApiException(message)
    class Client(message: String, statusCode: Int) : ApiException(message, statusCode)
    class Unauthorized(message: String) : ApiException(message, 401)
    class RateLimited(message: String) : ApiException(message, 429)
    class Server(message: String) : ApiException(message, 500)
    class Unknown(message: String) : ApiException(message)
}

object ApiErrorParser {
    fun parse(throwable: Throwable, moshi: Moshi): ApiException {
        return when (throwable) {
            is ApiException -> throwable
            is IllegalArgumentException ->
                ApiException.Client(throwable.message ?: "Xatolik", 400)
            is UnknownHostException, is SocketTimeoutException ->
                ApiException.Network("Internet aloqasi yo'q")
            is IOException ->
                ApiException.Network("Internet aloqasi yo'q")
            is HttpException -> {
                val code = throwable.code()
                val errorBody = throwable.response()?.errorBody()?.string()
                val message = parseMessage(errorBody, moshi) ?: defaultMessage(code)
                when (code) {
                    401 -> ApiException.Unauthorized(message)
                    429 -> ApiException.RateLimited(message)
                    in 500..599 -> ApiException.Server(message)
                    else -> ApiException.Client(message, code)
                }
            }
            else -> ApiException.Unknown(throwable.message ?: "Noma'lum xato")
        }
    }

    private fun parseMessage(body: String?, moshi: Moshi): String? {
        if (body.isNullOrBlank()) return null
        return try {
            moshi.adapter(ApiErrorDto::class.java).fromJson(body)?.message
        } catch (_: Exception) {
            null
        }
    }

    private fun defaultMessage(code: Int): String = when (code) {
        404 -> "Topilmadi"
        429 -> "Juda ko'p so'rov. Keyinroq urinib ko'ring."
        in 500..599 -> "Server xatosi. Keyinroq urinib ko'ring."
        else -> "Xatolik yuz berdi"
    }
}

suspend fun <T> safeApiCall(moshi: Moshi, block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(ApiErrorParser.parse(e, moshi))
    }
}
