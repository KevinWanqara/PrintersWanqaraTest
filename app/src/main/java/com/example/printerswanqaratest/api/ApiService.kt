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
import com.example.printerswanqaratest.api.sales.SalesService

// Data classes for login
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val data: UserData,
)

data class UserData(
    val token: String,
    val user: UserDetails
)
data class UserDetails(
    val id: String,
    val name: String,
    val email: String,

)

data class ResetPasswordRequest(val email: String)
data class ResetPasswordResponse(val message: String)

data class VerifyRucResponse(
    val data: RucData,
    val message: String
)

data class RucData(
    val id: String,
    val ruc: String,
    val name: String
)
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
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc") // Debug log to check RUC value
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json") // Add Accept header


                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc") // Debug log to confirm header addition
                    requestBuilder.addHeader("X-tenant", ruc.trim()) // Add X-tenant header if RUC is not null
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token") // Debug log to confirm header addition
                    requestBuilder.addHeader("Authorization", "Bearer $token") // Add Authorization header if token is not null
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

    fun createSalesService(context: Context): SalesService {
        val client = OkHttpClient.Builder()
            // ...add interceptors as needed...
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(SalesService::class.java)
    }
}
