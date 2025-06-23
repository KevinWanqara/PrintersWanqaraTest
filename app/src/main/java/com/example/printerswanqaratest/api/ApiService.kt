package com.example.printerswanqaratest.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.printerswanqaratest.data.AppStorage
import android.content.Context

// Data classes for login
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String)

data class ResetPasswordRequest(val email: String)
data class ResetPasswordResponse(val message: String)

data class VerifyRucResponse(val valid: Boolean, val ruc: String)

interface ApiService {
    @POST("general/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("general/forgot-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ResetPasswordResponse

    @GET("tenants/verify-ruc")
    suspend fun verifyRuc(@Query("ruc") ruc: String): VerifyRucResponse
}

object ApiClient {
    private const val BASE_URL = "https://system.wanqara.org/api/v1/"

    // Provide context when building the client
    fun createApiService(context: Context, withTenant: Boolean = true): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val requestBuilder = original.newBuilder()
                // Only add X-tenant header if requested and RUC is present
                if (withTenant && !ruc.isNullOrBlank()) {
                    requestBuilder.addHeader("X-tenant", ruc)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(ApiService::class.java)
    }
}
