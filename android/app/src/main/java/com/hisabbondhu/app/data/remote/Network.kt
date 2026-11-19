package com.hisabbondhu.app.data.remote

import com.hisabbondhu.app.BuildConfig
import com.hisabbondhu.app.data.prefs.SettingsStore
import com.hisabbondhu.app.data.remote.dto.ErrorResponse
import com.hisabbondhu.app.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

/** Raised when the refresh token is no longer valid and the user must log in again. */
class SessionExpiredException : IOException("session_expired")

class Network(private val settings: SettingsStore) {

    @Volatile private var cachedAccess: String? = null

    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("X-App-Version", BuildConfig.VERSION_NAME)
                    .build(),
            )
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    private val authInterceptor = Interceptor { chain ->
        val token = cachedAccess ?: runBlocking { settings.currentAuth().accessToken }.also { cachedAccess = it }
        val req = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(req)
    }

    private val retrofitBuilder = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))

    private val refreshApi: RefreshApi = retrofitBuilder.client(baseClient).build().create(RefreshApi::class.java)

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (responseCount(response) >= 2) return null
            synchronized(this) {
                val auth = runBlocking { settings.currentAuth() }
                val refresh = auth.refreshToken ?: return null
                val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                // Another thread already refreshed — retry with the new token.
                if (auth.accessToken != null && auth.accessToken != sentToken) {
                    cachedAccess = auth.accessToken
                    return response.request.newBuilder().header("Authorization", "Bearer ${auth.accessToken}").build()
                }
                return try {
                    val res = runBlocking { refreshApi.refresh(RefreshRequest(refresh)) }
                    runBlocking { settings.updateTokens(res.accessToken, res.refreshToken) }
                    cachedAccess = res.accessToken
                    response.request.newBuilder().header("Authorization", "Bearer ${res.accessToken}").build()
                } catch (e: HttpException) {
                    if (e.code() == 401 || e.code() == 403) {
                        runBlocking { settings.clearAuth() }
                        cachedAccess = null
                    }
                    null
                } catch (_: Exception) {
                    null
                }
            }
        }

        private fun responseCount(response: Response): Int {
            var r: Response? = response
            var count = 1
            while (r?.priorResponse != null) { count++; r = r.priorResponse }
            return count
        }
    }

    val api: HisabApi = retrofitBuilder
        .client(
            baseClient.newBuilder()
                .addInterceptor(authInterceptor)
                .authenticator(tokenAuthenticator)
                .build(),
        )
        .build()
        .create(HisabApi::class.java)

    fun onTokensChanged(access: String?) {
        cachedAccess = access
    }

    val isConfigured: Boolean
        get() = BuildConfig.API_BASE_URL.startsWith("https://") || BuildConfig.DEBUG
}

/** Extracts the server's machine-readable error code (e.g. "invalid_code") from an HTTP error. */
fun Throwable.apiErrorCode(): String? {
    if (this !is HttpException) return null
    val body = response()?.errorBody()?.string() ?: return "http_${code()}"
    return runCatching { AppJson.decodeFromString(ErrorResponse.serializer(), body).error }.getOrNull()
        ?: "http_${code()}"
}
