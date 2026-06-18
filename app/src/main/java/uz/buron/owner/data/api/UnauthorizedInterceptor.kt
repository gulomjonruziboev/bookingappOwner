package uz.buron.owner.data.api

import okhttp3.Interceptor
import okhttp3.Response
import uz.buron.owner.data.local.TokenStore
import javax.inject.Inject

class UnauthorizedInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            tokenStore.clear()
        }
        return response
    }
}
