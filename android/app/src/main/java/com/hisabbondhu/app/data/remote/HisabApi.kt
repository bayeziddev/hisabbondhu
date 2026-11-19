package com.hisabbondhu.app.data.remote

import com.hisabbondhu.app.data.remote.dto.AuthResponse
import com.hisabbondhu.app.data.remote.dto.BusinessDto
import com.hisabbondhu.app.data.remote.dto.ContactRequest
import com.hisabbondhu.app.data.remote.dto.OkResponse
import com.hisabbondhu.app.data.remote.dto.OtpRequest
import com.hisabbondhu.app.data.remote.dto.OtpRequestResponse
import com.hisabbondhu.app.data.remote.dto.PullRequest
import com.hisabbondhu.app.data.remote.dto.PullResponse
import com.hisabbondhu.app.data.remote.dto.PushRequest
import com.hisabbondhu.app.data.remote.dto.PushResponse
import com.hisabbondhu.app.data.remote.dto.RefreshRequest
import com.hisabbondhu.app.data.remote.dto.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HisabApi {
    @POST("v1/auth/request-otp")
    suspend fun requestOtp(@Body body: OtpRequest): OtpRequestResponse

    @POST("v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): AuthResponse

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): OkResponse

    @POST("v1/businesses/link")
    suspend fun linkBusiness(@Body body: BusinessDto): BusinessDto

    @PUT("v1/businesses/{id}")
    suspend fun updateBusiness(@Path("id") id: String, @Body body: BusinessDto): BusinessDto

    @POST("v1/businesses/{id}/sync/push")
    suspend fun push(
        @Path("id") businessId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PushRequest,
    ): PushResponse

    @POST("v1/businesses/{id}/sync/pull")
    suspend fun pull(@Path("id") businessId: String, @Body body: PullRequest): PullResponse

    @DELETE("v1/account")
    suspend fun deleteAccount(): OkResponse

    @POST("v1/public/contact")
    suspend fun contact(@Body body: ContactRequest): OkResponse
}

/** Separate interface used only by the token authenticator (no auth header, no recursion). */
interface RefreshApi {
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse
}
